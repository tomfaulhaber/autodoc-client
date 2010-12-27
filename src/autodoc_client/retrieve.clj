(ns autodoc-client.retrieve
  (:import [java.io LineNumberReader PushbackReader StringReader])
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [clj-http.client :as client]))

(defn retrieve 
  "Retrieve the autodoc index from repo (specified as something like 
\"http://github.com/clojure/clojure\") for the specified version ver"
  [repo ver]
  (->
   (client/get
    (str repo "/raw/gh-pages/index-" ver ".clj")
    {:accept :text})
   :body
   read-string))

(defn source-fn
  "Modified from clojure.repl/source-fn to pull the first source form 
out of a string"
  [s]
  (with-open [rdr (LineNumberReader. (StringReader. s))]
    (let [text (StringBuilder.)
          pbr (proxy [PushbackReader] [rdr]
                (read [] (let [i (proxy-super read)]
                           (.append text (char i))
                           i)))]
      (read (PushbackReader. pbr))
      (str text))))

(defn source-for 
  "Pull the source for each of the vars in the file"
  [file vars]
  (println "FILE:" file)
  (let [lines (str/split-lines 
               (:body (client/get file {:accept :text})))
        vars (sort-by :line vars)]
    (loop [vars vars
           pos (dec (:line (first vars)))
           lines (drop pos lines)
           acc []]
      ;;(println (:name (first vars)) (:line (first vars)) (:line (first (next vars))))
      ;; Sometime clojure decides that a whole bunch of vars are at the same line
      ;; (because it sets :line to the beginning of the top-level form, I think).
      ;; This difference is captured with chunk-size (the size of the item, which
      ;; may include multiple vars) and next-form which will be 0 when multiple
      ;; items are in the same place.
      (let [chunk-size (when-let [n (first
                                     (drop-while
                                      #(= (inc pos) (:line %)) (next vars)))] 
                         (- (:line n) pos 1))
            chunk (str/join "\n" 
                            (if chunk-size
                              (take chunk-size lines)
                              lines))
            acc (conj acc [((juxt :namespace :name) (first vars)) (source-fn chunk)])]
        (if (next vars)
          (let [next-form (- (:line (first (next vars))) pos 1)] 
            (recur (next vars) (+ pos next-form) (drop next-form lines) acc))
          acc)))))

(defn add-source
  "Add the source text and referenced symbols to the autodoc index structure"
  [doc]
  (let [vars-by-file (reduce (partial merge-with concat)
                             (for [v (:vars doc)] {(:raw-source-url v) [v]}))
        ;; _ (doseq [[file vars] vars-by-file] (println file))
        source-info (into {} 
                          (apply concat 
                                 (for [[file vars] vars-by-file :when file] 
                                   (source-for file vars))))]
    (update-in 
     doc [:vars] 
     #(for [var %] (assoc var :source-text (source-info [(:namespace var) (:name var)]))))))

(defn no-source
  "Find the vars in the doc structure that have no source text associated 
with them. This really only makes sense on a structure that's been
created by add-source "
  [doc]
  (map (comp (fn [[ns v]] (str ns "/" v))
             (juxt :namespace :name)) 
       (filter (complement :source-text) (:vars doc))))


