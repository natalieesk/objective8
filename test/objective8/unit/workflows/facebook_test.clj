(ns objective8.unit.workflows.facebook-test
  (:require [midje.sweet :refer :all]
            [objective8.front-end.workflows.facebook :refer :all]
            [cheshire.core :as json]
            [objective8.front-end.api.http :as http]
            [objective8.utils :as utils]))

(def fake-client-id "o8-id")
(def fake-client-secret "shh")
(def fake-request {:facebook-config {:client-id     fake-client-id
                                     :client-secret fake-client-secret}})
(def login-code "fb-code")

(def access-token "fb-token")
(def access-token-response {:body (json/generate-string {:access_token access-token})})
(def graph-api-error {:body (json/generate-string {:error {:code 190
                                                           :type "OAuthException"}})})

(def fb-user-id "facebook-1234567")
(defn token-info-response [& {:keys [user-id app-id expires_at]
                              :or   {user-id    1234567
                                     app-id     fake-client-id
                                     expires_at 9876543210}}]
  {:body (json/generate-string {:data {:user_id    user-id
                                       :app_id     app-id
                                       :expires_at expires_at}})})

(defn with-code [request]
  (assoc-in request [:params :code] login-code))

(defn with-error [request]
  (assoc-in request [:params :error] "access_denied"))

(fact "step 1: redirect to facebook auth page"
      (let [response (facebook-sign-in fake-request)]
        response => (contains {:status  302
                               :headers {"Location" (str "https://www.facebook.com/dialog/oauth?client_id=" fake-client-id
                                                         "&redirect_uri=" redirect-uri)}})))

;; (fact "step 2: login and permissions acceptance handled by facebook")

(facts "step 3: authenticating user"
       (fact "stores the facebook user id in the session and redirects to sign-up"
             (facebook-callback (-> fake-request with-code)) => (contains {:status  302
                                                                           :headers {"Location" (str utils/host-url "/sign-up")}
                                                                           :session {:auth-provider-user-id fb-user-id}})

             (provided
               (http/get-request "https://graph.facebook.com/v2.3/oauth/access_token" {:query-params {:client_id     fake-client-id
                                                                                                      :redirect_uri  redirect-uri
                                                                                                      :client_secret fake-client-secret
                                                                                                      :code          login-code}})
               => access-token-response
               (http/get-request "https://graph.facebook.com/debug_token" {:query-params {:input_token  access-token
                                                                                          :access_token (str fake-client-id "|" fake-client-secret)}})
               => (token-info-response)))
       (fact "redirects to homepage when user doesn't authorise application or error in fb"
             (facebook-callback (-> fake-request with-error)) => (contains {:status  302
                                                                            :headers {"Location" utils/host-url}}))
       (fact "redirects to homepage when access token returns an error"
             (facebook-callback (-> fake-request with-code)) => (contains {:status  302
                                                                           :headers {"Location" utils/host-url}})
             (provided
               (http/get-request "https://graph.facebook.com/v2.3/oauth/access_token" {:query-params {:client_id     fake-client-id
                                                                                                      :redirect_uri  redirect-uri
                                                                                                      :client_secret fake-client-secret
                                                                                                      :code          login-code}})
               => graph-api-error))
       (fact "redirects to homepage when debug token returns an error"
             (facebook-callback (-> fake-request with-code)) => (contains {:status  302
                                                                           :headers {"Location" utils/host-url}})
             (provided
               (http/get-request "https://graph.facebook.com/v2.3/oauth/access_token" {:query-params {:client_id     fake-client-id
                                                                                                      :redirect_uri  redirect-uri
                                                                                                      :client_secret fake-client-secret
                                                                                                      :code          login-code}})
               => access-token-response
               (http/get-request "https://graph.facebook.com/debug_token" {:query-params {:input_token  access-token
                                                                                          :access_token (str fake-client-id "|" fake-client-secret)}})
               => graph-api-error))
       (fact "redirects to homepage when app id in token info doesn't match client id"
             (facebook-callback (-> fake-request with-code)) => (contains {:status  302
                                                                           :headers {"Location" utils/host-url}})

             (provided
               (http/get-request "https://graph.facebook.com/v2.3/oauth/access_token" {:query-params {:client_id     fake-client-id
                                                                                                      :redirect_uri  redirect-uri
                                                                                                      :client_secret fake-client-secret
                                                                                                      :code          login-code}})
               => access-token-response
               (http/get-request "https://graph.facebook.com/debug_token" {:query-params {:input_token  access-token
                                                                                          :access_token (str fake-client-id "|" fake-client-secret)}})
               => (token-info-response :app-id "error")))
       (fact "redirects to homepage when expiry time in token info is after now"
             (facebook-callback (-> fake-request with-code)) => (contains {:status  302
                                                                           :headers {"Location" utils/host-url}})

             (provided
               (http/get-request "https://graph.facebook.com/v2.3/oauth/access_token" {:query-params {:client_id     fake-client-id
                                                                                                      :redirect_uri  redirect-uri
                                                                                                      :client_secret fake-client-secret
                                                                                                      :code          login-code}})
               => access-token-response
               (http/get-request "https://graph.facebook.com/debug_token" {:query-params {:input_token  access-token
                                                                                          :access_token (str fake-client-id "|" fake-client-secret)}})
               => (token-info-response :expires_at 111))))