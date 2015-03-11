(ns objective8.storage-helpers
  (:require [objective8.storage.storage :as storage]))


(def username-index (atom 0))

(defn generate-unique-username []
  (str "username-" (swap! username-index inc)))

(defn store-a-user [] (storage/pg-store! {:entity :user
                                          :twitter-id "twitter-TWITTER_ID"
                                          :username (generate-unique-username)}))

(defn store-an-objective []
  (let [{user-id :_id} (store-a-user)]
    (storage/pg-store! {:entity :objective
                        :created-by-id user-id
                        :end-date "2015-01-01"})))

(defn store-an-objective-in-draft []
  (let [{user-id :_id} (store-a-user)]
    (storage/pg-store! {:entity :objective
                        :created-by-id user-id
                        :end-date "2015-01-02"
                        :drafting-started true})))

(defn store-a-comment
  ([]
   (store-a-comment {:user (store-a-user) :objective (store-an-objective)}))

  ([required-entities]
   (let [{created-by-id :_id} (get required-entities :user (store-a-user))
         {objective-id :_id} (get required-entities :objective (store-an-objective))]
     (storage/pg-store! {:entity :comment
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :comment "The comment"}))))

(defn store-an-invitation
  ([] (store-an-invitation {}))

  ([required-entities]
   (let [{invited-by-id :_id} (get required-entities :user (store-a-user))
         {objective-id :_id} (get required-entities :objective (store-an-objective))
         status (get required-entities :status "active")]
     (storage/pg-store! {:entity :invitation
                         :uuid (java.util.UUID/randomUUID)
                         :status status
                         :invited-by-id invited-by-id
                         :objective-id objective-id
                         :reason "some reason"
                         :writer-name "writer name"}))))

(defn store-a-question
  ([]
   (store-a-question {}))

  ([required-entities]
   (let [{created-by-id :_id} (get required-entities :user (store-a-user))
         {objective-id :_id} (get required-entities :objective (store-an-objective))]
     (storage/pg-store! {:entity :question
                         :created-by-id created-by-id
                         :objective-id objective-id
                         :question "A question"}))))

(defn store-an-answer
  ([]
   (store-an-answer {}))

([required-entities]
 (let [{created-by-id :_id} (get required-entities :user (store-a-user))
       {objective-id :objective-id q-id :_id} (get required-entities :question (store-a-question))]
   (storage/pg-store! {:entity :answer
                       :created-by-id created-by-id
                       :objective-id objective-id
                       :question-id q-id
                       :answer "An answer"}))))

(defn store-a-candidate
  ([]
   (store-a-candidate {}))

  ([required-entities]
   (let [{user-id :_id} (get required-entities :user (store-a-user))
         {i-id :_id o-id :objective-id} (get required-entities :invitation (store-an-invitation))]
     (storage/pg-store! {:entity :candidate
                         :user-id user-id
                         :objective-id o-id
                         :invitation-id i-id}))))

(defn retrieve-invitation [invitation-id]
  (-> (storage/pg-retrieve {:entity :invitation :_id invitation-id}) 
      :result
      first))

(defn retrieve-latest-draft [objective-id]
  (-> (storage/pg-retrieve {:entity :draft :objective-id objective-id}
                           {:sort {:field :_created_at :ordering :DESC}})
      :result
      first))
