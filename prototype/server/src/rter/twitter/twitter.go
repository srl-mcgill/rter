// TODO: move config values to config file

package twitter

import (
	"bytes"
	"net/http"
	"rter/auth"
	"encoding/json"
	"errors"
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

func TwitterHandlerFunc(w http.ResponseWriter, r *http.Request) {
	user, permissions := auth.Challenge(w, r, true)
	if user == nil || permissions < 1 {
		http.Error(w, "Please Login", http.StatusUnauthorized)
		return
	}
	
	url := "https://api.twitter.com/1.1/search/tweets.json?count=40&callback=angular.callbacks._0&include_entities=true&q=fire&result_type=recent&geocode=45.56021795715051,-73.5774564743042,38.36127544070008km"
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		http.Error(w, "Failed to build request : " + err.Error(), http.StatusBadRequest)
		return
	}

	token, err = getToken()

	req.Header.Add("Authorization", "Bearer " + token.AccessToken)
	fmt.Printf("%v\n", req)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		http.Error(w, "Failed to send request: " + err.Error(), http.StatusBadRequest)
		return
	}
	fmt.Printf("%v\n", resp)

	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		http.Error(w, "Unexpected status received: " + resp.Status, http.StatusBadRequest)
		return
	}

	//TODO: this is not setting the Content-Type header in the response
	w.Header().Set("Content-Type", "application/json")
	//w.WriteHeader(http.StatusOK)
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