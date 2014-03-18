// TODO: move config values to config file

package twitter

import (
	"bytes"
	"net/http"
	"rter/auth"
	"encoding/json"
	"errors"
	"io"
	"strings"
	"fmt"
)

type twitterToken struct {
	AccessToken string `json:"access_token"`
	TokenType 	string `json:"token_type,omitempty"`
}

var token *twitterToken

func init() {
	token, _ = getToken()
}

type TwitterHandler struct {}

func NewTwitterHandler() *TwitterHandler {
	return new(TwitterHandler)
}

func (t *TwitterHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	TwitterHandlerFunc(w, r)
}

func TwitterHandlerFunc(w http.ResponseWriter, r *http.Request) {
	user, permissions := auth.Challenge(w, r, true)
	if user == nil || permissions < 1 {
		http.Error(w, "Please Login", http.StatusUnauthorized)
		return
	}

	twitterURL := "https://api.twitter.com" + r.URL.Path + "?" + r.URL.RawQuery
	req, err := http.NewRequest("GET", twitterURL, nil)
	if err != nil {
		http.Error(w, "Failed to build request : " + err.Error(), http.StatusBadRequest)
		return
	}

	if strings.HasPrefix(r.URL.Path, "/1.1") {
		token, err = getToken()

		req.Header.Add("Authorization", "Bearer " + token.AccessToken)
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		http.Error(w, "Failed to send request: " + err.Error(), http.StatusBadRequest)
		return
	}

	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		io.Copy(w, resp.Body)
		//http.Error(w, "Unexpected status received: " + resp.Status, http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	//encoder := json.NewEncoder(w)
	//err = encoder.Encode(token)
	//w.WriteHeader(http.StatusOK)

	io.Copy(w, resp.Body)
}

func getToken() (*twitterToken, error) {
	url := "https://api.twitter.com/oauth2/token"
	body := bytes.NewBufferString("grant_type=client_credentials")
	key := "n2clTepr1mcN7E1WJE4lg"
	secret := "FzmBeIvjbWQlXPMxy2GrVDvOfKoRN7toRzrsY5Wb0c"
	req, err := http.NewRequest("POST", url, body)
	req.SetBasicAuth(key, secret)
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		return nil, errors.New("Error: " + resp.Status)
	}

    token := new(twitterToken)
    decoder := json.NewDecoder(resp.Body)
	err = decoder.Decode(&token)
	if err != nil {
		return nil, errors.New("Invalid JSON received")
	}
	if token.TokenType != "bearer" {
		return nil, errors.New("Unexpected token type received: " + token.TokenType)
	}

	return token, nil
}

/*
func searchQuery(query string) (http.Response, error){

}
*/