(ns plaza.rdf.schemas-test
  (:use clojure.test
        plaza.rdf.core
        plaza.rdf.implementations.sesame
        plaza.rdf.schemas
        plaza.rdf.sparql
        midje.sweet)
  (:require [clojure.tools.logging :as log]))

(init-sesame-framework)
(use 'plaza.rdf.vocabularies.foaf)
(init-vocabularies)

(defonce *test-model* (make-rdfs-schema ["http://something/" "Good"]
                                        :name   {:uri "http://test.com/name"      :range :string}
                                        :price  {:uri ["http://test.com/" :price] :range :float}
                                        :number {:uri :number                     :range :int}))

(deftest test-extend-schemas
  (let [extended (extend-rdfs-schemas "http://test.com/Foo" [*test-model*])]
    (is (= "http://test.com/Foo" (str (type-uri extended))))
    (is (some (partial = "http://test.com/Foo")
              (map #(str %1) (super-type-uris extended)) ))
    (is (some (partial =  "http://something/Good")
              (map #(str %1) (super-type-uris extended))))
    (is (= "http://test.com/name" (str (property-uri extended :name))))))

(deftest test-props
  (is (= "http://something/Good" (str (type-uri *test-model*)))))

(deftest test-add-remove-prop
  (do (let [modelp (-> *test-model*
                       (add-property :wadus "http://test.com/wadus" :float)
                       (add-property :foo "http://test.com/foo" "http://test.com/ranges/foo"))
            modelpp (-> modelp
                        (remove-property-by-uri "http://test.com/foo")
                        (remove-property-by-alias :wadus))]
        (= :foo (property-alias modelp "http://test.com/foo"))
        (= :wadus (property-alias modelp "http://test.com/wadus"))
        (is (nil? (property-alias modelpp :wadus)))
        (is (nil? (property-alias modelpp :foo))))))

(deftest test->map
  (let [m (to-map *test-model* [[:test ["http://test.com/" :name] "name"]
                                [:test ["http://test.com/" :price] (d 120)]
                                [:test :number (d 10)]])]
    (is (= m {:name (rdf-resource "name") :price (d 120) :number (d 10)}))))

(deftest test->pattern
  (let [p (to-pattern *test-model* [:name :price])]
    (is (= 4 (count p)))
    (is (= 2 (count (filter #(:optional (meta %1)) p))))))

(deftest test->pattern-2
  (let [p (to-pattern *test-model* "http://test.com/Test" [:name :price])]
    (is (= 4 (count p)))
    (is (= 2 (count (filter #(:optional (meta %1)) p))))
    (doseq [[s _p _o] p] (is (= "http://test.com/Test" (resource-id s))))))

(deftest test-property-uri
  (is (= "http://test.com/name" (str (property-uri *test-model* :name)))))

(deftest test-property-alias
  (is (= :name (property-alias *test-model* "http://test.com/name"))))

(deftest test-property-parse-value
  (is (= 2 (parse-prop-value *test-model* :number "2"))))

(deftest test-schema->triples
  (let [ts (to-rdf-triples foaf:Agent-schema)]
    (is (= 37 (count ts)))))

(fact "parse-from-rdf"
  (init-sesame-framework)

  (let [ts (to-rdf-triples foaf:Agent-schema)
        *m* (build-model)
        _tmp (with-model *m* (model-add-triples ts))
        parsed (parse-rdfs-schemas-from-model *m*)]
    (sort (aliases (first parsed))) =>  (sort (aliases foaf:Agent-schema))))
