(ns studyflow.learning.course-material-test
  (:require [studyflow.learning.course-material :as material]
            [clojure.java.io :as io]
            [clojure.test :refer [is deftest testing]]
            [rill.uuid :refer [new-id]]
            [cheshire.core :as json]
            [studyflow.json-tools :refer [key-from-json]]))

(defn read-example-json
  []
  (json/parse-string (slurp (io/resource "dev/material.json")) key-from-json))

(deftest parsing-test
  (testing "parsing example json"
    (is (= (:name (material/parse-course-material (read-example-json)))
           "Counting")))

  (testing "throws exceptions when not valid"
    (is (thrown? RuntimeException (material/parse-course-material {:id "invalid" :name "Counting"})))))


(deftest test-tag-tree
  (let [pre "<p>some text before <b>with <u>HTML</u></b>Met inline inputs"
        svg0 "<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" style=\"width: 1.714ex; height: 3.143ex; vertical-align: -1.143ex; margin-top: 1px; margin-right: 0px; margin-bottom: 1px; margin-left: 0px; position: static; \" viewBox=\"0 -934.4385140278929 717.0889244992065 1376.6552026488378\" xmlns=\"http://www.w3.org/2000/svg\"><defs id=\"MathJax_SVG_glyphs\"><path id=\"MJMAIN-34\" stroke-width=\"10\" d=\"M462 0Q444 3 333 3Q217 3 199 0H190V46H221Q241 46 248 46T265 48T279 53T286 61Q287 63 287 115V165H28V211L179 442Q332 674 334 675Q336 677 355 677H373L379 671V211H471V165H379V114Q379 73 379 66T385 54Q393 47 442 46H471V0H462ZM293 211V545L74 212L183 211H293Z\"></path><path id=\"MJMAIN-36\" stroke-width=\"10\" d=\"M42 313Q42 476 123 571T303 666Q372 666 402 630T432 550Q432 525 418 510T379 495Q356 495 341 509T326 548Q326 592 373 601Q351 623 311 626Q240 626 194 566Q147 500 147 364L148 360Q153 366 156 373Q197 433 263 433H267Q313 433 348 414Q372 400 396 374T435 317Q456 268 456 210V192Q456 169 451 149Q440 90 387 34T253 -22Q225 -22 199 -14T143 16T92 75T56 172T42 313ZM257 397Q227 397 205 380T171 335T154 278T148 216Q148 133 160 97T198 39Q222 21 251 21Q302 21 329 59Q342 77 347 104T352 209Q352 289 347 316T329 361Q302 397 257 397Z\"></path></defs><g stroke=\"black\" fill=\"black\" stroke-width=\"0\" transform=\"matrix(1 0 0 -1 0 0)\"><g transform=\"translate(120,0)\"><rect stroke=\"none\" width=\"477\" height=\"60\" x=\"0\" y=\"220\"></rect><use transform=\"scale(0.7071067811865476)\" href=\"#MJMAIN-34\" x=\"84\" y=\"611\" xlink:href=\"#MJMAIN-34\"></use><use transform=\"scale(0.7071067811865476)\" href=\"#MJMAIN-36\" x=\"84\" y=\"-571\" xlink:href=\"#MJMAIN-36\"></use></g></g></svg>"
        svg1 "<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" style=\"width: 1.714ex; height: 3.143ex; vertical-align: -1.143ex; margin-top: 1px; margin-right: 0px; margin-bottom: 1px; margin-left: 0px; position: static; \" viewBox=\"0 -934.4385140278929 717.0889244992065 1376.6552026488378\" xmlns=\"http://www.w3.org/2000/svg\"><defs id=\"MathJax_SVG_glyphs\"><path id=\"MJMAIN-34\" stroke-width=\"10\" d=\"M462 0Q444 3 333 3Q217 3 199 0H190V46H221Q241 46 248 46T265 48T279 53T286 61Q287 63 287 115V165H28V211L179 442Q332 674 334 675Q336 677 355 677H373L379 671V211H471V165H379V114Q379 73 379 66T385 54Q393 47 442 46H471V0H462ZM293 211V545L74 212L183 211H293Z\"></path><path id=\"MJMAIN-36\" stroke-width=\"10\" d=\"M42 313Q42 476 123 571T303 666Q372 666 402 630T432 550Q432 525 418 510T379 495Q356 495 341 509T326 548Q326 592 373 601Q351 623 311 626Q240 626 194 566Q147 500 147 364L148 360Q153 366 156 373Q197 433 263 433H267Q313 433 348 414Q372 400 396 374T435 317Q456 268 456 210V192Q456 169 451 149Q440 90 387 34T253 -22Q225 -22 199 -14T143 16T92 75T56 172T42 313ZM257 397Q227 397 205 380T171 335T154 278T148 216Q148 133 160 97T198 39Q222 21 251 21Q302 21 329 59Q342 77 347 104T352 209Q352 289 347 316T329 361Q302 397 257 397Z\"></path></defs><g stroke=\"black\" fill=\"black\" stroke-width=\"0\" transform=\"matrix(1 0 0 -1 0 0)\"><g transform=\"translate(90,0)\"><rect stroke=\"none\" width=\"477\" height=\"60\" x=\"0\" y=\"220\"></rect><use transform=\"scale(0.7071067811865476)\" href=\"#MJMAIN-34\" x=\"84\" y=\"611\" xlink:href=\"#MJMAIN-34\"></use><use transform=\"scale(0.7071067811865476)\" href=\"#MJMAIN-36\" x=\"84\" y=\"-571\" xlink:href=\"#MJMAIN-36\"></use></g></g></svg>"
        post "
<table><tr><td>_INPUT_1_ en </td><td><i> text _INPUT_2_ text</i></td></tr></table></p>

                     <img src=\"https://docs.google.com/drawings/d/1cP-5PnZA-jvGTDL9D9C6eZt-wxuTLNbKPJRpfU6kxxc/pub?w=735&h=260\">\r\n\r\n<p class=\"small\">Als je aantallen bij elkaar neemt noem je dat <b>optellen</b>. <br>\r\nHet teken <b>+</b> noem je <b>plus</b> of het plusteken.<br>\r\nMet ‘<b>de som</b>’ wordt de uitkomst na het optellen bedoeld.</p>\r\n\r\n<h1>Volgorde van optellen</h1>\r\n\r\n<p>Wanneer je twee getallen optelt maakt de volgorde niet uit:<br>\r\n<span class='inline_math'>5 + 7  =  7 + 5  =  12</span></p>\r\n\r\n<img src=\"https://docs.google.com/drawings/d/1xYdvVdENGvcrt2fTT8jJT5z1uCdF2awrxmVxP0IDqnc/pub?w=611&h=112\">\r\n\r\n"
        reflection-tag " _REFLECTION_1_"
        text (str pre svg0 svg1 reflection-tag post)
        custom-tag-names #{"_INPUT_1_" "_INPUT_2_" "_REFLECTION_1_"}]
    (is (not= svg0 svg1))
    (is (= (material/text-with-custom-tags-to-tree text custom-tag-names)
           {:tag :div,
            :attrs nil,
            :content
            [{:tag :div,
              :attrs {:class " div-p"},
              :content
              ["some text before "
               {:tag :b, :attrs nil, :content ["with " {:tag :u, :attrs nil, :content ["HTML"]}]}
               "Met inline inputs"
               {:tag :svg, :attrs {:name "_SVG_0_"}, :content svg0}
               {:tag :svg, :attrs {:name "_SVG_1_"}, :content svg1}
               " "
               {:tag :reflection,
                :attrs {:name "_REFLECTION_1_"},
                :content nil}
               "\n"
               {:tag :table,
                :attrs nil,
                :content
                [{:tag :tr,
                  :attrs nil,
                  :content
                  [{:tag :td, :attrs nil, :content [{:tag :input, :attrs {:name "_INPUT_1_"}, :content nil} " en "]}
                   {:tag :td,
                    :attrs nil,
                    :content [{:tag :i, :attrs nil, :content [" text " {:tag :input, :attrs {:name "_INPUT_2_"}, :content nil} " text"]}]}]}]}]}
             "\n\n                     "
             {:tag :img,
              :attrs {:src "https://docs.google.com/drawings/d/1cP-5PnZA-jvGTDL9D9C6eZt-wxuTLNbKPJRpfU6kxxc/pub?w=735&h=260"},
              :content nil}
             "\n\n"
             {:tag :div,
              :attrs {:class "small div-p"},
              :content
              ["Als je aantallen bij elkaar neemt noem je dat "
               {:tag :b, :attrs nil, :content ["optellen"]}
               ". "
               {:tag :br, :attrs nil, :content nil}
               "\nHet teken "
               {:tag :b, :attrs nil, :content ["+"]}
               " noem je "
               {:tag :b, :attrs nil, :content ["plus"]}
               " of het plusteken."
               {:tag :br, :attrs nil, :content nil}
               "\nMet ‘"
               {:tag :b, :attrs nil, :content ["de som"]}
               "’ wordt de uitkomst na het optellen bedoeld."]}
             "\n\n"
             {:tag :h1, :attrs nil, :content ["Volgorde van optellen"]}
             "\n\n"
             {:tag :div,
              :attrs {:class " div-p"},
              :content
              ["Wanneer je twee getallen optelt maakt de volgorde niet uit:"
               {:tag :br, :attrs nil, :content nil}
               "\n"
               {:tag :span, :attrs {:class "inline_math"}, :content ["5 + 7  =  7 + 5  =  12"]}]}
             "\n\n"
             {:tag :img,
              :attrs {:src "https://docs.google.com/drawings/d/1xYdvVdENGvcrt2fTT8jJT5z1uCdF2awrxmVxP0IDqnc/pub?w=611&h=112"},
              :content nil}
             "\n\n"]}))

    (let [table-text "<table class=\"m-table\">
                             <tr><td>Answer</td><td>Input</td></tr>
                             <tr><td>123
                               <img src=\"//some-file-somewhere/file.png\" width=\"60\" height=\"80\" style=\"width: 60%\">
<iframe width=\"560\" height=\"315\" src=\"//www.youtube-nocookie.com/embed/Lm4oiLzJs2g?rel=0\" frameborder=\"0\" allowfullscreen></iframe>
                             </td><td>_INPUT_1_</td></tr>
                             <tr><td>456</td><td>_INPUT_2_</td></tr></table>"]
      (is (= (material/text-with-custom-tags-to-tree table-text custom-tag-names)
             '{:tag :div,
              :attrs nil,
              :content
              ({:tag :table,
                :attrs {:class "m-table"},
                :content
                ("\n                             "
                 {:tag :tr, :attrs nil, :content ({:tag :td, :attrs nil, :content ("Answer")} {:tag :td, :attrs nil, :content ("Input")})}
                 "\n                             "
                 {:tag :tr,
                  :attrs nil,
                  :content
                  ({:tag :td,
                    :attrs nil,
                    :content
                    ("123\n                               "
                     {:tag :img, :attrs {:style {"width" "60%"}, :height "80", :width "60", :src "//some-file-somewhere/file.png"}, :content nil}
                     "\n"
                     {:tag :iframe,
                      :attrs {},
                      :content
                      "<iframe allowfullscreen=\"allowfullscreen\" frameborder=\"0\" src=\"//www.youtube-nocookie.com/embed/Lm4oiLzJs2g?rel=0\" height=\"315\" width=\"560\"></iframe>"}
                     "\n                             ")}
                   {:tag :td, :attrs nil, :content ({:tag :input, :attrs {:name "_INPUT_1_"}, :content nil})})}
                 "\n                             "
                 {:tag :tr,
                  :attrs nil,
                  :content
                  ({:tag :td, :attrs nil, :content ("456")}
                   {:tag :td, :attrs nil, :content ({:tag :input, :attrs {:name "_INPUT_2_"}, :content nil})})})})})))))
