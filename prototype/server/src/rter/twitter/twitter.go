package twitter

import (
	"bytes"
	"net/http"
	"rter/auth"
	"encoding/json"
	"errors"
)

type tokenResponse struct {
	Token 		string `json:"access_token"`
	TokenType 	string `json:"token_type,omitempty"`
}

func TokenHandlerFunc(w http.ResponseWriter, r *http.Request) {
	user, permissions := auth.Challenge(w, r, true)
	if user == nil || permissions < 1 {
		http.Error(w, "Please Login", http.StatusUnauthorized)
		return
	}
	
	token, err := getToken()
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	encoder := json.NewEncoder(w)
	err = encoder.Encode(token)
	if err != nil {
		http.Error(w, "Couldn't build JSON: " + token.TokenType, http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
}

func getToken() (*tokenResponse, error) {
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

    token := new(tokenResponse)
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