(ns midi-color-init
  (:require
   ;; javascript
   ["fs" :as fs]
   ["@tonejs/midi" :refer (Midi) :as midi]
   [chroma-js :as chroma]
   ;; clojurescript
   [cljs.reader :as reader]
   [goog.string :as gstring]
   [goog.string.format]
   [clojure.walk :as cw]
   [cognitect.transit :as t]
   [com.rpl.specter :as s]
   [clojure.string :as st]))

;; user settings
(def file-index 3)
(def hue-range 360)
(def lightness-range 150)
(def chroma-val 20)
(def input-midis-path "midi_files/")
(def output-overview-path "out/midi_data.json")

;; constants
(defn round
  [x] (/ (.round js/Math (* 100 x)) 100))
(def chromatic-scale ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])
(def chromatic-scale-factor
  (let [hue-range hue-range
        scale-count (count chromatic-scale)]
    (round (/ hue-range scale-count))))
(def chromatic-scale-to-hue-map
  (into {} (map-indexed (fn [idx itm] [itm (* (+ idx 1) chromatic-scale-factor)]) chromatic-scale)))
(def octaves [0 1 2 3 4 5 6 7 8])
(def octaves-factor
  (let [lightness-range lightness-range
        octave-count (count octaves)]
    (round (/ lightness-range octave-count))))
(def octaves-to-lightn-map
  (into {} (map (fn [x] [x (round (* (+ 1 x) octaves-factor))]) octaves)))
(defn produce-color
  [note octave]
  (.rgb (.lch chroma (get octaves-to-lightn-map octave) chroma-val (get chromatic-scale-to-hue-map note))))

;; inputs
(def input-midi-files
  (into [] (map (fn [filename] (Midi. (.readFileSync fs (st/join [input-midis-path filename])))) (js->clj (.readdirSync fs input-midis-path)))))

;; input transformation
(defn json-to-clj
  [json]
  (js->clj (.parse js/JSON (.stringify js/JSON json))))
(defn extract-notes
  [data]
  (get-in (first (get-in data ["tracks"])) ["notes"]))
(defn round-vals
  [notes]
  (cw/keywordize-keys (map (fn [x]
                             ;; TODO improve
                             (-> x
                                 (assoc "velocity" (reader/read-string (gstring/format "%.3f" (x "velocity"))))
                                 (assoc "duration" (reader/read-string (gstring/format "%.1f" (x "duration"))))
                                 (assoc "time" (reader/read-string (gstring/format "%.1f" (x "time"))))))
                           notes)))
(defn add-count
  [notes]
  (map-indexed (fn [i x] (assoc x :number (+ i 1))) notes))
(defn group-by-timestamp
  [notes]
  (->> (group-by :time notes)
       (map (fn [[k v]]
              [k (mapv (fn [x] (select-keys x [:name :duration :number])) v)]))))
(defn expand-note-to-lch
  [note-str]
  (let [[note octave]
        (if (= (count note-str) 3)
          [(subs note-str 0 2) (js/parseInt (subs note-str 2))]
          [(subs note-str 0 1) (js/parseInt (subs note-str 1))])]
    {:color (js->clj (produce-color note octave))}))
(defn add-color-to-notes
  [notes]
  (s/transform [s/ALL 1 s/ALL] #(merge % (expand-note-to-lch (:name %))) notes))
(def notes-transformed (->> (get input-midi-files file-index)
                            json-to-clj
                            extract-notes
                            round-vals
                            add-count
                            group-by-timestamp
                            ;; debugging
                            ;(#(doto % pp/pprint))
                            add-color-to-notes))

;; output transformation
(def max-duration
  (apply max (map (fn [x] (let [timestamp (first x)
                                max-note-dur (apply max (map (fn [x] (:duration x)) (second x)))]
                            (+ timestamp max-note-dur))) notes-transformed)))
(def num-notes
  (:number (first (second (last notes-transformed)))))

;; end result collection
(def output-overview
  {:max-duration max-duration
   :num-notes num-notes
   :notes-transformed (add-color-to-notes notes-transformed)})

(.writeFileSync fs output-overview-path (t/write (t/writer :json) output-overview))

(print "done")
