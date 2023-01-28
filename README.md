# midi-viz

- TODO

## quickstart
- install the dependencies as usual with leiningen and shadow-cljs
- build the update script in the `midi-color-transformer` dir with `npx shadow-cljs release midi-color-updater`
- open cljs and clj repl

## disclaimer
- this is a small project used for experimenting with Clojure, ClojureScript, Quil and their interactions to produce generative art
- I wanted to test the feasibility of functional programming for basic graphical computation as an alternative to standard OOP
- it is by no means complete, but I was trying to adhere to idiomatic principles in the languages I was using

## introduction
- the application uses both ClojureScript and Clojure to visualize .mid files in a straightforward manner using LCH colorspace
- ClojureScript is utilized in `midi-color-transformer` directory for processing of MIDI data for example exported by Ableton
- the rationale for using ClojureScript was to be able to leverage the JS libraries `tonal` for midi parsing and `chroma-js` for working with lch colorspace
- Clojure is used in `main-quil` directory, responsible for drawing via Quil, a Processing (P5) wrapper
- The midi files to be translated have to be placed in the `midi-color-transformer/midi_files` directory
- the app is designed to be used with two REPLs for clj and cljs
- there are three parts:

## midi_color_init.cljs
- reads in .mid file, does data transformation, assigns the respective color for a given note and octave and outputs result
- there are some user settings available in the namespace for experimentation
- you have to select the index of the midi file in `midi_files` you want to translate

## midi_viz/main.clj
- this is the main application used for drawing
- it uses the dataset produced in `midi_color_init.cljs` as a baseline
- you can manipulate lightness, chroma and hue values with the keys q, w, a, s, y and x
- to achieve this, I have to communicate with the JS libraries again, so a subset of data is manipulated by

## midi_color_updater.cljs
- a node.js CLI that receives colors vectors from `main.clj` and increments or decrements certain components of the lch color
- outputs new color vectors that are then displayed

