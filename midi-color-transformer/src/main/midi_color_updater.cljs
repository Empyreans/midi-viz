(ns midi-color-updater
  (:require [chroma-js :as chroma]
            ["fs" :as fs]
            [cljs.reader :as reader]))

(defn rgb-to-lch
  [r g b op amount]
  (let [[l c h] (js->clj (.lch (.rgb chroma r g b)))]
    (cond
      (= op :l) (js->clj (.rgb (.lch chroma (+ l amount) c h)))
      (= op :c) (js->clj (.rgb (.lch chroma l (+ c amount) h)))
      (= op :h) (js->clj (.rgb (.lch chroma l c (+ h amount)))))))

(defn main [& cli-args]
  (let [input-path (first cli-args)
        op (reader/read-string (second cli-args))
        amount (js/parseInt (nth cli-args 2))
        data (reader/read-string (.toString (.readFileSync fs input-path)))
        transform (map (fn [[r g b]] (rgb-to-lch r g b op amount)) data)
        output (str transform)]
    (prn output)))
