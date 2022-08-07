(ns ca.landonjw.api.text
  (:require [clojure.string :as str]
            [ca.landonjw.api.collection :refer [one-of?]])
  (:import (net.minecraft.util.text Color StringTextComponent TextFormatting)))

(def colors
  {:dark-red     (TextFormatting/DARK_RED)
   :red          (TextFormatting/RED)
   :gold         (TextFormatting/GOLD)
   :yellow       (TextFormatting/YELLOW)
   :dark-green   (TextFormatting/DARK_GREEN)
   :green        (TextFormatting/GREEN)
   :aqua         (TextFormatting/AQUA)
   :dark-aqua    (TextFormatting/DARK_AQUA)
   :dark-blue    (TextFormatting/DARK_BLUE)
   :blue         (TextFormatting/BLUE)
   :light-purple (TextFormatting/LIGHT_PURPLE)
   :dark-purple  (TextFormatting/DARK_PURPLE)
   :white        (TextFormatting/WHITE)
   :gray         (TextFormatting/GRAY)
   :dark-gray    (TextFormatting/DARK_GRAY)
   :black        (TextFormatting/BLACK)})

(def formats
  [:bold
   :underline
   :strikethrough
   :obfuscated])

(defn- color-keyword? [keyword]
  (one-of? (keys colors) keyword))

(def hex-regex #"^#[0-9A-Fa-f]{6}$")

(defn- hex-keyword? [keyword]
  "Checks if a keyword is a valid hex code in the form of :#000000 - :#FFFFFF.
  The keyword must have the the full 6 hex digits, and the alphabetical characters are case insensitive."
  (not (nil? (re-matches hex-regex (name keyword)))))

(defn- format-keyword? [keyword]
  (one-of? formats keyword))

(defn- keyword->rgb [hex-keyword]
  "Converts a hex keyword into an integer representation of an RGB value.
  If the keyword supplied is not valid, this will return nil."
  (when (hex-keyword? hex-keyword)
    (let [name (name hex-keyword)
          hex-code (str/replace name #"#" "")]
      (Long/parseLong hex-code 16))))

(defn- rgb->color [rgb]
  "Converts an integer RGB representation into a Minecraft color."
  (Color/fromRgb rgb))

(defn- get-color [keyword]
  "Gets a Minecraft color object from a keyword.
  This keyword can be one defined in `formats` or a hexcode in the form of `:#000000`-`:#FFFFFF`
  If the keyword supplied is invalid, this will return nil."
  (cond
    (color-keyword? keyword) (get colors keyword)
    (hex-keyword? keyword) (-> keyword keyword->rgb rgb->color)))

(defn- get-format-style [format text]
  "Gets a style for the specified format keyword.
  This will inherit any styles from the given `text` argument."
  (condp = format
    :bold (-> text .getStyle (.withBold true))
    :underline (-> text .getStyle (.withUnderlined true))
    :strikethrough (-> text .getStyle (.setStrikethrough true))
    :obfuscated (-> text .getStyle (.setObfuscated true))))

(defn- get-color-style [color text]
  "Gets a style for the specified color keyword.
  This will inherit any styles from the given `text` argument."
  (-> text .getStyle (.withColor (get-color color))))

(defn- apply-style [style text]
  "Applies a style to the supplied text component, depending on the keyword supplied."
  (if (format-keyword? style)
    (.setStyle text (get-format-style style text))
    (.setStyle text (get-color-style style text))))

(defn- append-text-and-style [contents text]
  "Creates a new text component, applies the style of the prior text component,
  and concatenates the two components together."
  (let [sibling (StringTextComponent. contents)]
    (.setStyle sibling (.getStyle text))
    (.append text sibling)))

(defn clj->text-component
  "Takes a vector and parses the form into a Minecraft TextComponent.
  Example vector: `[[:red \"Hello \" [:bold \"world! \"]] \"This is a test!\"]`"
  ([text]
   (clj->text-component text (StringTextComponent. "")))
  ([text acc]
   (doseq [element text]
     (cond
       (vector? element) (.append acc (clj->text-component element (StringTextComponent. "")))
       (keyword? element) (apply-style element acc)
       (string? element) (append-text-and-style element acc)))
   acc))