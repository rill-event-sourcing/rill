(ns studyflow.school-administration.import-student-test
  (:require [studyflow.school-administration.import-student :refer [import-rows import-tabbed-string full-name]]
            [studyflow.school-administration.student.events :as student-events]
            [studyflow.school-administration.department.events :as department]
            [rill.message :as message]
            [rill.temp-store :refer [given]]
            [rill.uuid :refer [new-id]]
            [clojure.string :as string]
            [clojure.test :refer :all]))

(def import-data
  [["Voornaam"  "Tussenvoegsel" "Achternaam" "Email"                "Wachtwoord" "Klas"]
   ["Joost"     ""              "Diepenmaat" "joost@studyflow.nl"   "12345"      "Havo-1"]
   ["Steven"    "van der"       "Thonus"     "stevent@studyflow.nl" "12345"      "Havo-1"]])


(defn fixup-events
  [events]
  (map (fn [e]
         (-> e
             (dissoc :student-id)
             (update-in [:credentials] dissoc :encrypted-password)))
       events))

(def department-id (new-id))
(def school-id (new-id))

(deftest test-importing
  (let [store (given [(department/created department-id school-id "DEPT")])
        report (import-rows store department-id import-data)]
    (is (= 2 (:total-students report)))
    (is (= 2 (:total-imported report)))
    (is (every? (fn [[status events]]
                  (and (= :ok status)
                       (= ::student-events/Imported (message/type (last events)))))
                (:results report)))

    (testing "running import twice"
      (let [report (import-rows store department-id import-data)]
        (is (= 0 (:total-imported report)))
        (is (= 2 (count (:errors report))))))))


(def tabbed-string
  (string/join
   "\r\n"
   ["Voornaam	Tussenvoegsel	Achternaam	Email	Wachtwoord (optioneel)	Klas (optioneel)	Email coach #1 (optioneel)	Email coach #2 (optioneel)	Email coach #... (optioneel)	Email coach #... (optioneel)	Email coach #... (optioneel)	Email coach #... (optioneel)"
    "Rico	de	Bruijn	rico.de.bruijn@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Pim		Hoek	pim.hoek@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Jaycee		Klees	Jayceeklees@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Emiel		Wilde	emiel.wilde@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Leonne		Oonk	leonne.oonk@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Kim	van de	Riet	kim.van.de.riet@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Max		Dekker	max.dekker@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Lieke		Thijs	lieke.thijs@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Myrthe		Weijermars	myrthe.weijermars@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Thu		Pham	thu.pham@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "   "
    "Joost		Diepenmaat			12345	3 Havo"
    "Luus		Marsman	luus.marsman@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Emma		Lammertink	emma.lammertink@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"
    "Daan		Jansen	daan.jansen@reggesteyn.nu	studyflow	3 Havo	l.meijerink@reggesteyn.nl"]))

(deftest test-importing-tabbed-data
  (let [store (given [(department/created department-id school-id "DEPT")])
        report (import-tabbed-string store department-id tabbed-string)]
    (is (= 14 (:total-students report)))
    (is (= 13 (:total-imported report)))))

(deftest test-full-name
  (is (= (full-name "Fred" nil "Flintstone")
         "Fred Flintstone"))
  (is (= (full-name "Fred" "" "Flintstone")
         "Fred Flintstone"))
  (is (= (full-name "Fred" "van der" "Flintstone")
         "Fred van der Flintstone")))
