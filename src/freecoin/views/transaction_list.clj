;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Arjan Scherpenisse <arjan@scherpenisse.net>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns freecoin.views.transaction-list
  (:require [freecoin.routes :as routes]
            [environ.core :as env]
            [simple-time.core :as st]
            [freecoin-lib.db.wallet :as wallet]
            [freecoin.form_helpers :as fh]))

(defn build-html [list tags wallet-store & [owner-wallet]]
  (let [title (str "Transaction list"
                   (when (not (nil? owner-wallet))
                     (str " for " (:email owner-wallet))))
        all-tags (reduce into #{} (map :tags list))
        tag-filter (fn [t]
                     [:option {:value t
                               :selected (some #(= t %) tags)} t])]
    {:title title
     :heading title
     :body-class "func--transactions-page--body"
     :body
     [:div
      [:form {:method "get"
              :class "form-shell"}
       (when (not (nil? owner-wallet))
         [:input {:name "email"
                  :type "hidden"
                  :value (:email owner-wallet)}])
       [:fieldset {:class "fieldset--filter-by-tags"}
        [:div {:class "form-group"}
         [:label {:class "control-label"
                  :for "filter-by-tags"} "Filter by tags:"]
         [:select.form-control
          {:id "filter-by-tags"
           :multiple true
           :name "tags"
           :size (min (count all-tags) 5)}
          (map tag-filter all-tags)]]]


        [:fieldset {:class "fieldset-submit"}
         [:div {:class "form-group"}
          [:span {:class "visible-xs-inline-block visible-sm-inline-block visible-md-inline-block visible-lg-inline-block"}
           [:input {:class "form-control btn btn-primary"
                    :id "field-submit"
                    :name "submit"
                    :type "submit"}]]]]
       ]

      [:table.func--transactions-page--table.table.table-striped
       [:thead
        [:tr
         [:th "From"]
         [:th "To"]
         [:th "Amount"]
         [:th "Time"]
         [:th "Tags"]]]
       [:tbody
        (map (fn [t]
               (let [from (wallet/fetch wallet-store (:from-id t))
                     to (wallet/fetch wallet-store (:to-id t))
                     tag (fn [t] [:span.tag [:a {:href (routes/path :get-tag-details :name t)} t]])]
                 [:tr
                  [:td [:a {:href (routes/path :account :email (:email from))} (:name from)]]
                  [:td [:a {:href (routes/path :account :email (:email to))} (:name to)]]
                  [:td (fh/thousand-separator (:amount t))]
                  [:td (-> t :timestamp st/parse (st/format :medium-date-time))]
                  [:td (interpose ", " (map tag (:tags t)))]]))
             list)]]]}))

(defn transaction->activity-stream [tx wallet-store]
  (let [from (wallet/fetch wallet-store (:from-id tx))
        to (wallet/fetch wallet-store (:to-id tx))]
    {"@context"   "https://www.w3.org/ns/activitystreams"
     "type"      "Transaction"
     "published" (str (:timestamp tx) "Z")

     "actor"     {"type" "Person"
                  "name" (:name from)}

     "target"    {"type" "Person"
                  "name" (:name to)}

     "object"    {"type" (:blockchain tx)
                  "name" (str (:amount tx))
                  "url" (str (env/env :base-url) "/transactions/" (:_id tx))}}))

(defn build-activity-stream [list wallet-store]
  {"@context"   "https://www.w3.org/ns/activitystreams"
   "type"       "Container"
   "name"       "Activity stream"
   "totalItems" (count list)
   "items"      (map #(transaction->activity-stream % wallet-store) list)})
