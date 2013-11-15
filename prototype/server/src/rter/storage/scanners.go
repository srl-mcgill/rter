package storage

import (
	"database/sql"
	"rter/data"
)

func scanItemComment(comment *data.ItemComment, rows *sql.Rows) error {
	err := rows.Scan(
		&comment.ID,
		&comment.ItemID,
		&comment.Author,
		&comment.Body,
		&comment.UpdateTime,
	)

	return err
}

func scanItem(item *data.Item, rows *sql.Rows) error {
	err := rows.Scan(
		&item.ID,
		&item.Type,
		&item.Author,
		&item.ThumbnailURI,
		&item.ContentURI,
		&item.UploadURI,
		&item.ContentToken,
		&item.HasHeading,
		&item.Heading,
		&item.HasGeo,
		&item.Lat,
		&item.Lng,
		&item.Radius,
		&item.Live,
		&item.StartTime,
		&item.StopTime,
	)

	return err
}

func scanGeolocation(geolocation *data.Geolocation, rows *sql.Rows) error {
	var (
		lat sql.NullFloat64
		lng sql.NullFloat64
		heading sql.NullFloat64
		radius sql.NullFloat64
	)

	err := rows.Scan(
		&geolocation.ItemID,
		&lat,
		&lng,
		&heading,
		&radius,
		&geolocation.Timestamp,
	)

	if err != nil {
		return err
	}

	// Don't assign if NULL
	if lat.Valid == true {
		geolocation.Lat = lat.Float64
	}
	if lng.Valid == true {
		geolocation.Lng = lng.Float64
	}
	if heading.Valid == true {
		geolocation.Heading = heading.Float64
	}
	if radius.Valid == true {
		geolocation.Radius = radius.Float64
	}

	return nil
}

func scanTerm(term *data.Term, rows *sql.Rows) error {
	cols, err := rows.Columns()

	if err != nil {
		return err
	}

	if len(cols) < 5 {
		err = rows.Scan(
			&term.Term,
			&term.Automated,
			&term.Author,
			&term.UpdateTime,
		)
	} else {
		err = rows.Scan(
			&term.Term,
			&term.Automated,
			&term.Author,
			&term.UpdateTime,
			&term.Count,
		)
	}

	return err
}

func scanTermRelationship(relationship *data.TermRelationship, rows *sql.Rows) error {
	err := rows.Scan(
		&relationship.Term,
		&relationship.ItemID,
	)

	return err
}

func scanTermRanking(ranking *data.TermRanking, rows *sql.Rows) error {
	err := rows.Scan(
		&ranking.Term,
		&ranking.Ranking,
		&ranking.UpdateTime,
	)

	return err
}

func scanRole(role *data.Role, rows *sql.Rows) error {
	err := rows.Scan(
		&role.Title,
		&role.Permissions,
	)

	return err
}

func scanUser(user *data.User, rows *sql.Rows) error {
	err := rows.Scan(
		&user.Username,
		&user.Password,
		&user.Salt,
		&user.Role,
		&user.TrustLevel,
		&user.CreateTime,
		&user.Heading,
		&user.Lat,
		&user.Lng,
		&user.UpdateTime,
		&user.Status,
		&user.StatusTime,
	)

	return err
}

func scanUserDirection(direction *data.UserDirection, rows *sql.Rows) error {
	err := rows.Scan(
		&direction.Username,
		&direction.LockUsername,
		&direction.Command,
		&direction.Heading,
		&direction.Lat,
		&direction.Lng,
		&direction.UpdateTime,
	)

	return err
}
