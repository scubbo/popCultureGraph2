Definitions:

Title       - Movie or TV Show.
Actor       - Person, male or female, who acts in a Title. I'm all for equality, but
              Naming Things is srs bizness.
Populated   - All neighbours of the node have been added to the graph
Spread      - All neighbours of all neighbours of the node have been added to the graph
              (i.e. all neighbours of the node are populated)
Sociability - The number of films an actor has been in, or the number of actors in a film.
              so, yeah, the degree. Screw you, mathematicians, I can make up my own words!


Here's what happens when you click a (title) node:

* RequestHandler calls GraphAdapter.getSociableNeighboursOfTitle
* GraphAdapter checks that the title's been spread
* Gets the neighbourList of the title
* Determine the low and high sociability boundaries for display
 * TODO - currently, this only works for one "level" - should make multiple clicks tick to next level
* Return the neighbours with appropriate sociability levels to RequestHandler
* RequestHandler begins constructing a JSON object to return
* RequestHandler calls GraphAdapter.spreadFromActorNode for every appropriately-sociable actor
 * TODO - this should totally be done on a background thread!
 * Would need some means of blocking (/erroring) if a later request is for a node that's being spread from
