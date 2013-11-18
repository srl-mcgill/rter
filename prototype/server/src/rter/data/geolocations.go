// Provides datastructures and associated func for rtER
//
// The datastructures reflect the core information we are storing and manipulating in the rtER project.
package data

import (
	"strconv"
	"time"
)

type Geolocation struct {
	ItemID    int64     //Unique of the associated Item. Tied to Item.ID in DB
	Lat       *float64   `json:",omitempty"`
	Lng       *float64   `json:",omitempty"`
	Heading   *float64   `json:",omitempty"`
	Radius    *float64   `json:",omitempty"`
	Timestamp *time.Time `json:",omitempty"`
}

func (g *Geolocation) CRUDPrefix() string {
	return "items/" + strconv.FormatInt(g.ItemID, 10) + "/geolocations"
}

func (g *Geolocation) CRUDPath() string {
	return g.CRUDPrefix()
}