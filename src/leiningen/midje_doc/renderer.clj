(ns leiningen.midje-doc.renderer
  (:require [hiccup.core :as html]
            [markdown.core :refer [md-to-html-string]]
            [me.raynes.conch :refer [programs]]))

(programs pygmentize)

(def ^:dynamic *plain* true)

(defn adjust-fact-code [s spaces]
  (let [i-arrow (.lastIndexOf s "=>")]
    (if (<= 0 i-arrow)
        (let [i-nl  (.lastIndexOf s "\n" i-arrow)
              rhs (-> (.substring s i-nl)
                      (.replaceAll (str "\n" spaces) "\n"))
              lhs (.substring s 0 i-nl)]
          (str lhs rhs))
        s)))

(defn render-element [elem]
  (condp = (:type elem)

    :chapter
    [:div
     (if (:tag elem) [:a {:name (:tag elem)}])
     [:h2 [:b (str (:num elem) " &nbsp;&nbsp; " (:title elem))]]]
    :section
    [:div
     (if (:tag elem) [:a {:name (:tag elem)}])
     [:h3 (str (:num elem) " &nbsp;&nbsp; " (:title elem))]]
    :subsection
    [:div
     (if (:tag elem) [:a {:name (:tag elem)}])
     [:h3 [:i (str (:num elem) " &nbsp;&nbsp; " (:title elem))]]]
    :subsubsection
    [:div
     (if (:tag elem) [:a {:name (:tag elem)}])
     [:h3 [:i (str (:num elem) " &nbsp;&nbsp; " (:title elem))]]]
    :paragraph [:div (md-to-html-string (:content elem))]
    :image
    [:div {:class "figure"}
     (if (:tag elem) [:a {:name (:tag elem)}])
     (if (:num elem)
       [:h4 [:i (str "fig." (:num elem)
                     (if-let [t (:title elem)] (str "  &nbsp;-&nbsp; " t)))]])
     [:div {:class "img"} [:img (dissoc elem :num :type :tag)]]
     [:p]]
    :ns
    [:div
     (if *plain*
       [:pre (:content elem)]
       (pygmentize  "-f" "html" "-l" (or (:lang elem) "clojure")
                    {:in (:content elem)}))]
    :code
    [:div
     (if (:tag elem) [:a {:name (:tag elem)}])
     (if (:num elem)
       [:h4 [:i (str "e." (:num elem)
                     (if-let [t (:title elem)] (str "  &nbsp;-&nbsp; " t)))]])
     (if *plain*
       (pygmentize  "-f" "html" "-l" (or (:lang elem) "clojure")
                    {:in (adjust-fact-code (:content elem)
                                           (apply str (repeat (or (:fact-level elem) 0) "  ")))})
       [:pre (adjust-fact-code (:content elem)
                               (apply str (repeat (or (:fact-level elem) 0) "  ")))])]))


(defn render-toc-element [elem]
  (case (:type elem)
    :chapter [:h4
              [:a {:href (str "#" (:tag elem))} (str (:num elem) " &nbsp; " (:title elem))]]

    :section [:h5 "&nbsp;&nbsp;"
              [:i [:a {:href (str "#" (:tag elem))} (str (:num elem) " &nbsp; " (:title elem))]]]

    :subsection [:h5 "&nbsp;&nbsp;&nbsp;&nbsp;"
                [:i [:a {:href (str "#" (:tag elem))} (str (:num elem) " &nbsp; " (:title elem))]]]))

(defn render-toc [elems]
  (let [telems (filter #(#{:chapter :section :subsection} (:type %)) elems)]
    (map render-toc-element telems)))


(defn render-elements [elems]
  (html/html
   (map render-element elems)))

(defn slurp-res [path]
  (slurp (or (clojure.java.io/resource path)
             (str "resource/" path))))

(defn render-heading [document]
  [:div {:class "heading"}
   [:h1 (:title document)]
   [:h3 (:sub-title document)]
   [:hr]
   [:div {:class "info"}
    (if-let [author (:author document)]
      [:h5 "Author: " author
       (if-let [email (:email document)]
         [:b "&nbsp;&nbsp;" [:a {:href (str "mailto:" email)} "(" email ")"] ])])
    (if-let [version (:version document)]
      [:h5 "Library: v" version])
    (if-let [rev (:revision document)]
      [:h5 "Revision: v" rev])
    [:h5 "Date: " (.format
                   (java.text.SimpleDateFormat. "dd MMMM YYYY")
                   (java.util.Date.))]]
   [:br]
   [:hr]])

(defn render-html-doc [output document elems]
  (let [heading (render-heading document)]
    (spit output
          (html/html
           [:html
            [:head
             [:meta {:http-equiv "X-UA-Compatible" :content "chrome=1"}]
             [:meta {:charset "utf-8"}]
             [:meta {:name "viewport" :content "width=device-width, initial-scale=1, user-scalable=no"}]
             [:title (or (:window document) (:title document))]
             [:style
              (str
               (slurp-res "template/stylesheets/styles.css")
               "\n\n"
               (slurp-res "template/stylesheets/pygment_trac.css")
               "\n\n")]]
            [:body
             [:header
              heading
              (render-toc elems)
              [:br]]
             [:section
              heading
              (map render-element elems)]]
            [:script {:type "text/javascript"}
             (slurp-res "template/javascripts/scale.fix.js")]]))))
