package panorama

import (
	//"log"
	"math"
	"encoding/json"
	"rter/data"
	"rter/storage"
)

type PanoramaUpdater struct {

}

func NewPanoramaUpdater() *PanoramaUpdater {
	p := new(PanoramaUpdater)
	storage.AddListener(p)
	return p
}

func (p *PanoramaUpdater) InsertEvent(v interface{}) {
	
}

func (p *PanoramaUpdater) UpdateEvent(v interface{}) {
	_, ok := v.(*data.Item)
	
	if !ok {
		return
	}

	matchingItems := make([]*data.Item, 0)
	storage.SelectQuery(&matchingItems, `
		SELECT i.* 
		FROM Items AS i 
		LEFT JOIN TermRelationships AS tr
		ON i.ID = tr.ItemID
		WHERE tr.Term = 'panorama'
		AND i.Type = 'streaming-video-v1'
		ORDER BY i.StartTime DESC
		LIMIT 0,3
	`)

	newOrder := make([]int64, 0)
	if len(matchingItems) == 2 {
		if math.Mod(matchingItems[0].Heading - matchingItems[1].Heading + 360, 360) > 180 {
			newOrder = append(newOrder, matchingItems[0].ID, matchingItems[1].ID)
		} else {
			newOrder = append(newOrder, matchingItems[1].ID, matchingItems[0].ID)
		}
	} else if len(matchingItems) == 3 {
		angle01 := AngleBetween(matchingItems[0].Heading, matchingItems[1].Heading)
		angle02 := AngleBetween(matchingItems[0].Heading, matchingItems[2].Heading)
		angle12 := AngleBetween(matchingItems[1].Heading, matchingItems[2].Heading)
		if angle01 > angle02 && angle01 > angle12 {
			if math.Mod(matchingItems[0].Heading - matchingItems[1].Heading + 360, 360) > 180 {
				newOrder = append(newOrder, matchingItems[0].ID, matchingItems[2].ID, matchingItems[1].ID)
			} else {
				newOrder = append(newOrder, matchingItems[1].ID, matchingItems[2].ID, matchingItems[0].ID)
			}
		} else if angle02 > angle01 && angle02 > angle12 {
			if math.Mod(matchingItems[0].Heading - matchingItems[2].Heading + 360, 360) > 180 {
				newOrder = append(newOrder, matchingItems[0].ID, matchingItems[1].ID, matchingItems[2].ID)
			} else {
				newOrder = append(newOrder, matchingItems[2].ID, matchingItems[1].ID, matchingItems[0].ID)
			}
		} else{
			if math.Mod(matchingItems[1].Heading - matchingItems[2].Heading + 360, 360) > 180 {
				newOrder = append(newOrder, matchingItems[1].ID, matchingItems[0].ID, matchingItems[2].ID)
			} else {
				newOrder = append(newOrder, matchingItems[2].ID, matchingItems[0].ID, matchingItems[1].ID)
			}
		}
	}

	ranking := new(data.TermRanking)
	ranking.Term = "panorama"
	rankingJson, err := json.Marshal(newOrder)
	if err != nil {
		return
	}
	ranking.Ranking = string(rankingJson)

	storage.Update(ranking)

	/*
	var rankings data.TermRanking
	rankings.Term = "panorama"
	err := storage.Select(&rankings)
	if err != nil {
		log.Printf("%#v\n", err)
		return
	}

	order := make([]int, 0)
	err = json.Unmarshal([]byte(rankings.Ranking), &order)
	if err != nil {
		log.Printf("%#v\n", err)
		return
	}

	log.Printf("%#v\n", order)
	*/


}

func (p *PanoramaUpdater) DeleteEvent(v interface{}) {

}

func Start() {
	NewPanoramaUpdater()
}

func AngleBetween(a float64, b float64) float64 {
	angle := math.Mod(math.Abs(a - b), 360)
	if angle > 180 {
		return 360 - angle
	}
	return angle
}