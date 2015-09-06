;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Duncan Mortimer <dmortime@thoughtworks.com>

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

(ns freecoin.db.mongo
  (:require [monger.collection :as mc]
            [monger.core :as mongo]))

(def ^:private participant-collection "participants")
(def ^:private wallet-collection "wallets")

(defprotocol FreecoinStore
  (store! [e k item]
    "Store item against the key k")
  (fetch [e k]
    "Retrieve item based on primary id")
  (query [e query]
    "Items are returned using a query map"))

(defrecord MongoStore [mongo-db coll]
  FreecoinStore
  (store! [this k item]
    (-> (mc/insert-and-return mongo-db coll (assoc item :_id (k item)))
        (dissoc :_id)))
  (fetch [this k]
    (when k
      (-> (mc/find-map-by-id mongo-db coll k)
          (dissoc :_id))))
  (query [this query]
    (->> (mc/find-maps mongo-db coll query)
         (map #(dissoc % :_id)))))

(defn create-mongo-store [mongo-db coll]
  (MongoStore. mongo-db coll))

(defrecord MemoryStore [data]
  FreecoinStore
  (store! [this k item]
    (do (swap! data assoc (k item) item)
        item))
  (fetch [this k] (@data k))
  (query [this query]
    (filter #(= query (select-keys % (keys query))) (vals @data))))

(defn create-memory-store
  "Create a memory store"
  ([] (create-memory-store {}))
  ([data]
   (MemoryStore. (atom data))))

(defn create-participant-store [db]
  (create-mongo-store db participant-collection))

(defn create-wallet-store [db]
  (create-mongo-store db wallet-collection))