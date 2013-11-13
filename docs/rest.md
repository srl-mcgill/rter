# REST API

## Resources

Datatypes are:
* items
* users
* roles
* taxonomy

Childtypes are:
* ranking
* direction
* comments

|Request Method|Resource|Description|
|---|---|---|
|GET|/1.0/{datatype:items|users|roles|taxonomy}|Returns a collection of all items.|
|GET|/1.0/{datatype:items|users|roles|taxonomy}/{id}|Returns the item specified by {id}.|
|POST|/1.0/{datatype:items|users|roles|taxonomy}|Creates a new item and returns created item. Request body can contain any fields being initialized.|
|PUT|/1.0/{datatype:items|users|roles|taxonomy}/{id}|Updates the item specified by {id} and returns it if it has been modified. Request body should contain any fields being updated.|
|DELETE|/1.0/{datatype:items|users|roles|taxonomy}/{id}|Deletes the item specified by {id}.|
|GET|/1.0/{datatype:items|users|roles|taxonomy}/{id}/{childtype:ranking|direction|comments}|Returns a collection of children of item specified by {id}.|
|PUT|/1.0/{datatype:items|users|roles|taxonomy}/{id}/{childtype:ranking|direction}|Updates the item specified by {id} and returns it if it has been modified. Request body should contain any fields being updated.|
|POST|/1.0/{datatype:items|users|roles|taxonomy}/{id}/comments|Creates a new comment with parent item specified by {id}. Request body can contain any fields being initialized.|
|GET|/1.0/{datatype:items|users|roles|taxonomy}/{id}/{childtype:comments}/{childid}|Returns comment specified by {childid} that is a child of item {id}|
|PUT|/1.0/{datatype:items|users|roles|taxonomy}/{id}/{childtype:comments}/{childid}|Updates comment specified by {childid} that is a child of item {id}, and returns it if it has been modified. Request body should contain any fields being updated.|
|DELETE|/1.0/{datatype:items|users|roles|taxonomy}/{id}/{childtype:comments}/{childkey}|Deletes comment specified by {childid} that is a child of {id}|