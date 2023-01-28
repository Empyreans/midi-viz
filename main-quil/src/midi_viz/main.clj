(ns midi-viz.main
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [cognitect.transit :as t]
            [clojure.java [io :as io]]
            [clojure.java.shell :as sh]))

;; user settings
(def window-w 2000)
(def window-h 2000)
(def note-data (t/read (t/reader (io/input-stream "../midi-color-transformer/out/midi_data.json") :json)))
(def updater-exec-path "../midi-color-transformer/out/midi_color_updater.js")
(def temp-output-path "target/transit_data")

(defn grid-dims-for-num
  "Given a number of elements, return the dimensions of the grid to display most elements in as even dimensions as possible."
  [num]
  (loop [i 1
         x num
         dif (- x i)
         res [i x dif]]
    (if (<= dif -2)
      [(first res) (second res)]
      (recur (+ i 1)
             (int (/ num (+ i 1)))
             (- (int (/ num (+ i 1))) (+ i 1))
             (if (< dif (nth res 2))
               [i x dif]
               res)))))

(defn make-grid-dim
  "Parts the dim into num pieces"
  [dim num]
  (take-nth (/ dim num) (range dim)))

(defn get-new-color
  [op amount colors]
  (let [op-arg (str op)
        amount-arg (str amount)]
    (spit temp-output-path colors)
    (into [] (read-string (read-string (:out (sh/sh "node" updater-exec-path temp-output-path op-arg amount-arg)))))))

(defn update-color-state
  [state op amount]
  (let [colorz (into [] (map :color state))
        f #(get-new-color op amount %)
        updated (f colorz)]
    (map (fn [old new] (assoc old :color new)) state updated)))

(def initial-state
  (let [[grid-w grid-h] (grid-dims-for-num (:num-notes note-data))
        y-dim (make-grid-dim window-w grid-w)
        x-dim (make-grid-dim window-h grid-h)
        y-r (second y-dim)
        x-r (second x-dim)
        x-vals (flatten (map #(repeat (count y-dim) %) x-dim))
        y-vals y-dim
        origins (partition 2 (interleave x-vals (cycle y-vals)))
        cells (let [a (map (fn [[x y]] {:x x :y y :x-r x-r :y-r y-r}) origins)
                    b (flatten (map (fn [x] (second x)) (:notes-transformed note-data)))]
                (map merge a b))]
    cells))

(defn on-key-pressed [state event]
  (case (:key event)
    (:q) (update-color-state state :l 5)
    (:w) (update-color-state state :l -5)
    (:a) (update-color-state state :c 5)
    (:s) (update-color-state state :c -5)
    (:y) (update-color-state state :h 10)
    (:x) (update-color-state state :h -10)
    state))

(defn setup []
  (q/frame-rate 30)
  initial-state)

(defn draw-state [state]
  (q/background 255)
  (q/no-stroke)

  ; center
  (q/scale 0.5 0.5)
  (q/translate (/ window-h 2) (/ window-w 2))

  (update-color-state state :h 100)

  ; iterate over cells
  (doseq [c state]
    (q/fill (:color c))
    (q/rect
     (:x c)
     (:y c)
     (+ 1 (:x-r c))
     (+ 1 (:y-r c)))))

(q/defsketch midi-viz
  :host "host"
  :size [window-w window-h]
  :setup setup
  :key-pressed on-key-pressed
  ;:update update-state
  :draw draw-state
  :middleware [m/fun-mode])
