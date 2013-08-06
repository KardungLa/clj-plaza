(ns plaza.rdf.core-test
  (:use (plaza.rdf core)
        (plaza.rdf.implementations jena common)
        clojure.test
        midje.sweet))

;; rdf/xml used in the tests
(def ^:dynamic *test-xml* "<rdf:RDF
    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"
    xmlns:test=\"http://plaza.org/ontologies/\" >
  <rdf:Description rdf:about=\"http://plaza.org/ontologies/a\">
    <test:c rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</test:c>
    <test:b rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">2</test:b>
  </rdf:Description>
  <rdf:Description rdf:about=\"http://plaza.org/ontologies/d\">
    <test:e rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</test:e>
  </rdf:Description>
</rdf:RDF>")

(def ^:dynamic *test-xml-blanks*
  "<?xml version=\"1.0\"?>
<rdf:RDF xmlns:csf=\"http://schemas.microsoft.com/connectedservices/pm#\"
         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"
         xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"
         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">
  <rdf:Description rdf:about=\"urn:upn_abc\">
    <csf:Phone>
      <rdf:Description>
        <csf:Phone-Home-Primary>425-555-0111</csf:Phone-Home-Primary>
        <csf:Phone-Mobile-Other>425-555-0114</csf:Phone-Mobile-Other>
        <csf:Phone-Office-Other>425-555-0115</csf:Phone-Office-Other>
      </rdf:Description>
    </csf:Phone>
  </rdf:Description>
</rdf:RDF>")

;; we'll test with jena
(init-jena-framework)

(fact "create-model-jena"
  (build-model :jena) => model?)


(fact "with-rdf-ns"
  (let [before *rdf-ns*
        new-ns "hello"]
    (with-rdf-ns new-ns *rdf-ns*) => new-ns
    *rdf-ns* => before))

(fact "with-model"
  (let [before-ns *rdf-ns*
        before-model *rdf-model*
        new-ns "hello"
        new-model "bye"]

    (with-rdf-ns new-ns
      (with-model new-model
        *rdf-ns* => new-ns
        *rdf-model* => new-model))

    *rdf-ns* => before-ns
    *rdf-model* => before-model))

(fact "make-property"
  (with-model  (build-model :jena)
    (to-string (rdf-property rdf :hola)) => "http://www.w3.org/1999/02/22-rdf-syntax-ns#hola"
    (to-string (rdf-property rdf:type)) => "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))

(deftest test-make-resource
  (let [m (build-model :jena)
        p1 (with-model m
             (rdf-resource rdf :Mundo))
        p2 (with-model m
             (rdf-property rdfs:Class))]
    (is (= (to-string p1) "http://www.w3.org/1999/02/22-rdf-syntax-ns#Mundo"))
    (is (= (to-string p2) "http://www.w3.org/2000/01/rdf-schema#Class"))))


(deftest test-make-literal
  (let [m (build-model :jena)
        p1 (with-model m
             (rdf-literal "test"))
        p2 (with-model m
             (rdf-literal "test" "es"))]
    (is (= (to-string p1) "test"))
    (is (= (to-string p2) "test@es"))))

(deftest test-make-typed-literal
  (let [m (build-model :jena)
        p1 (with-model m
             (rdf-typed-literal (Integer. 2)))
        p2 (with-model m
             (rdf-typed-literal 2 :anyuri))]
    (is (= (to-string p1) "\"2\"^^<http://www.w3.org/2001/XMLSchema#int>"))
    (is (= (to-string p2) "\"2\"^^<http://www.w3.org/2001/XMLSchema#anyURI>"))))

(deftest test-triple-subject
  (let [m (build-model :jena)
        p1 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-subject :A)))
        p2 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-subject [rdf :A])))]
    (is (= (to-string p1) "http://test.com/A"))
    (is (= (to-string p2) "http://www.w3.org/1999/02/22-rdf-syntax-ns#A"))))

(deftest test-triple-predicate
  (let [m (build-model :jena)
        p1 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-predicate :p)))
        p2 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-subject [rdf :p])))]
    (is (= (to-string p1) "http://test.com/p"))
    (is (= (to-string p2) "http://www.w3.org/1999/02/22-rdf-syntax-ns#p"))))

(deftest test-triple-object
  (let [m (build-model :jena)
        p1 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-object :p)))
        p2 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-object [rdf :p])))
        p3 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-object (l "test"))))
        p4 (with-model m
             (with-rdf-ns "http://test.com/"
               (triple-object (d (Integer. 2)))))]
    (is (= (to-string p1) "http://test.com/p"))
    (is (= (to-string p2) "http://www.w3.org/1999/02/22-rdf-syntax-ns#p"))
    (is (= (to-string p3) "test"))
    (is (= (to-string p4) "\"2\"^^<http://www.w3.org/2001/XMLSchema#int>"))))

(deftest test-rdf-triple-a
  (let [m (build-model :jena)
        ts (with-model m
             (with-rdf-ns "http://test.com/"
               (rdf-triple [:a :b :c])))]
    (is (= (count ts) 3))
    (is (= (to-string (nth ts 0)) "http://test.com/a"))
    (is (= (to-string (nth ts 1)) "http://test.com/b"))
    (is (= (to-string (nth ts 2)) "http://test.com/c"))))

(deftest test-rdf-triple-b
  (let [m (build-model :jena)
        ts (with-model m
             (with-rdf-ns "http://test.com/"
               (rdf-triple [:a  [:b :c
                                 :d :e]])))]
    (is (= (count ts) 2))
    (let [fts (nth ts 0)
          sts (nth ts 1)]
      (is (= (to-string (nth fts 0)) "http://test.com/a"))
      (is (= (to-string (nth fts 1)) "http://test.com/b"))
      (is (= (to-string (nth fts 2)) "http://test.com/c"))
      (is (= (to-string (nth sts 0)) "http://test.com/a"))
      (is (= (to-string (nth sts 1)) "http://test.com/d"))
      (is (= (to-string (nth sts 2)) "http://test.com/e")))))

(deftest test-add-triples
  (let [m (build-model :jena)]
    (with-model m (model-add-triples [[:a :b :c] [:d :e :f] [:g [:h :i :j :k]]]))
    (is (= 4 (count (walk-triples m (fn [s p o] [s p o])))))))

(deftest test-add-triples-2
  (let [m (build-model :jena)]
    (with-model m (model-add-triples (make-triples [[:a :b :c] [:d :e :f] [:g [:h :i :j :k]]])))
    (is (= 4 (count (walk-triples m (fn [s p o] [s p o])))))))

(deftest test-remove-triples-1
  (let [m (defmodel
             (model-add-triples (make-triples [[:a :b (d 2)]]))
             (model-add-triples (make-triples [[:e :f (l "test")]])))]
    (do (with-model m (model-remove-triples (make-triples [[:a :b (d 2)]])))
        (= 1 (count (model->triples m))))))

(deftest test-optional
  (let [optional? (optional [:foo])]
    (is (:optional (meta (first optional?))))))

(deftest test-optional-2
  (let [optional? (optional [:foo :bar])
        opt? (opt [:foo :bar])]
    (is (= optional? opt?))))

(deftest test-document->model-1
  (let [m (build-model :jena)
        _m (with-model m (document->model (java.io.ByteArrayInputStream. (.getBytes *test-xml*)) :xml))]
    (is (= (count (model->triples m)) 3))))

(deftest test-document->model-2
  (let [m (build-model :jena)
        _m (with-model m (document->model (java.io.ByteArrayInputStream. (.getBytes *test-xml-blanks*)) :xml))]
    (is (= (count (model->triples m)) 4))
    (is (or (bnode? (o (first (model->triples m))))
            (bnode? (o (second (model->triples m))))
            (bnode? (o (nth (model->triples m) 2)))
            (bnode? (o (nth (model->triples m) 3)))))))

(deftest test-find-resources
  (let [m (build-model :jena)
        _m (with-model m (document->model (java.io.ByteArrayInputStream. (.getBytes *test-xml*)) :xml))
        res (find-resources m)]
    (is (= (count res) 2))))

(deftest test-find-resource-uris
  (let [m (build-model :jena)
        _m (with-model m (document->model (java.io.ByteArrayInputStream. (.getBytes *test-xml*)) :xml))
        res (find-resource-uris m)]
    (is (= (count res) 2))))

(deftest test-blank-node
  (let [b1 (blank-node)
        b2 (b)
        b3 (blank-node :a)
        b4 (b :a)]
    (is (bnode? b1))
    (is (bnode? b2))
    (is (bnode? b3))
    (is (bnode? b4))
    (is (= :a (keyword (blank-node-id b3))))
    (is (= :a (keyword (blank-node-id b4))))))

(deftest test-blank-node-is
  (is (not (bnode? :?a)))
  (is (not (bnode? (d 2))))
  (is (not (bnode? (l "test"))))
  (is (not (bnode? (rdf-resource "http://test.com/Test")))))

(deftest test-has-meta
  (is (:triples (meta (make-triples [[:a :b :c]])))))

(deftest test-suppored-datatype
  (is (supported-datatype? :int))
  (is (not (supported-datatype? :foo))))

(deftest datatype-uri-test
  (is (= "http://www.w3.org/2001/XMLSchema#double" (datatype-uri :double))))
