Here's what you need to do:
* Returning just the first N actors from a title is fine because they're already sorted by importance (actually by appearances, I think?)
* Alternative source for titles-for-actor is http://www.imdb.com/filmosearch?explore=title_type&role=nm0277213&ref_=filmo_vw_adv&sort=user_rating,desc&mode=advanced&page=1&title_type=tvSeries, but doesn't list character names - should be able to backfetch that, though?
 * Needs to search both Movies *and* TV (separate pages)
 * Limited to 50 results per page :(
* Still need to allow multi-clicking for "next 3", and to suppress based on already-existing nodes
* Longer-term, it would be nice if edges got drawn for any already existing nodes (i.e. if A1 and A2 were in T1 and T2, and current graph state is T1-A1-T2, spreading from T2 should a) create T2-A2 and b) create A2-T1)


IGNORE THE BELOW - it's still mostly true but in the process of being refactored

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
