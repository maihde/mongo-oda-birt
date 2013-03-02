About
=====

This project includes patches that I have made to the mongo-oda-birt-plugin
http://code.google.com/a/eclipselabs.org/p/mongo-oda-birt-plugin/. 

The examples data set is downloaded from [http://media.mongodb.org/zips.json](http://media.mongodb.org/zips.json)
and imported into your MongoDB instance using this command:

    mongoimport --db test --collection zips --file zips.json

Change Log
==========

Support Regular MongoDB Query Syntax
====================================

When constructing a query, you can now use regular MongoDB syntax.

    db.collection.find( <query>, <projection> )

For example, to query all elements in the dataset use: 

    db.zips.find()

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot001.png)

But you can perform filtering on the server:

    db.zips.find({state: "IA"})

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot002.png)

This can be benefical/necessary on very large databases.

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot003.png)

All valid MongoDB query statements should be supported, see the [MongoDB
Guide](http://docs.mongodb.org/manual/core/read-operations/) for further
details on the syntax of advanced query operations.

The query may include both a filter's and 
When you specify a query using MongoDB syntax (i.e. is starts with "db.")
additional features will be enabled.  If you specify the query as the
collection name, then the legacy 1.0.0 behavior is used.

As part of the query, you can also specify the "projection" to be used, which will
limit the returned columns.  For example:
    
    db.zips.find({state: "IA"}, {"city": 1, "pop": 1})

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot004.png)

Will produce:

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot005.png)

To use a projection without a filter, use this syntax:
    
    db.zips.find({ }, {"city": 1, "pop": 1})

Work Correctly With Heterogenous Documents
==========================================
Because MongoDB is schema-free, not all documents in the collection will have
the same set of keys.  With version 1.0.0 of mongo-oda-birt, the first document
in the collection was used as the 'prototype'.  If you don't provide any column
restrictions or a query projection, all columns across the entire data-set will
be available.  For example, add the following into the 'zips' collection:

$ mongo
MongoDB shell version: 2.0.4
connecting to: test
> db.zips.insert({"field1": "X", "field2": "Y", "field3": "Z"})

The new columns will now be available in your data-set.

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot006.png)

Easier Access to FilterCriteria and SortCriteria
================================================
In 1.0.0, FilterCriteria and SortCriteria were tied to DataSet parameters.
This caused two problems:
* The DataSet wizard would issue ominous errors about vaguly named parameters
  not having a default value, and 
* Constructing the JSON statement as part of the report parameter was awkward
  for users, especially if multiple report parameters were to be combined into
  a single filter.

Of course, this is only necessary if you want to filter on the server-side.
Alternatively you can filter on the client side as either part of the data-set
or as part of your report design.  The client-side filters provided by BIRT
have more capabilities that those provided by Mongo, so it's often easier to
used those.  The server-side filters are useful to reduce the amount of data
that is fetched and processed.

In 1.1.0, when you use the new db.<collection>.find() query syntax these
parameters are not longer used.  For backwards compatibility, these are still
used if you define a query with the legacy syntax.

Now you can specify these criteria as properties:

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot007.png)

A simple criteria can look like this:

    ({state: params["State"].value}).toSource()

This will generally work, except for when the report parameter is "null", the
behavior will be to find all documents that have now value for the state
column.  This can be especially problematic if you have a report parameter with
a dynamic lookup.

Although more verbose, it's better to define your FilterCriteria in this manner.

    filter = {}
    if (params["State"].value) {
       filter.state = params["State"].value
    }
    if (params["MinimumPopulation"].value) {
       filter.pop = params["MinimumPopulation"].value
    }
    filter.toSource()

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot008.png)

Results in:

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot009.png)

IMPORTANT: If you specify a FilterCriteria property value, the filter criteria
(if any) specified in the original query is ignored.  In legacy mode, the
FilterCriteria parameter has higher priority than the FilterCriteria property.

SortCriteria can be specified in the same manner.  For example, to create a static sort (note that
the critera field was set to be a constant instead of a JavaScript expression).  

![ScreenShot](http://raw.github.com/maihde/mongo-oda-birt/master/screenshots/screenshot010.png)

Inferred Data-types Follow Types in the Database
================================================
In 1.0.0, date fields would be treated as strings.  In 1.1.0, date fields are
correctly inferred to be dates.  In addition, in 1.0.0 a string "0.123" would
be inferred to be a number.  In 1.1.0 a string is considered a string
regardless of it's content.  You can always adjust the column types manually if
necessary.  If the first document doesn't include all of the columns, the types
will initially be set to be a string.

Fixing Default Logging
================================================
By default, 1.0.0 would produce a log with the .metadata area of your workspace
as the FINEST level of logging.  As such, this file could grow very large and
it could potentially reveal sensitive data (since it is dumping all of the conents
of the database operations to the log).

In addition, when the ODA plugin is used on Linux within an application-server
(i.e. Tomcat) you cannot specify a default log directory of "C:\temp".

Making the UI more Friendly
================================================
I've made minor changes to help make the UI more intuitive.

Created a Feature and Update Site
================================================
To make it easier to install, an update site is now available.  Point to this update
site to install:

[http://raw.github.com/maihde/mongo-oda-birt/master/org.eclipse.birt.report.data.oda.mongodb.site](http://raw.github.com/maihde/mongo-oda-birt/master/org.eclipse.birt.report.data.oda.mongodb.site)

Or download an archived update-site:
[org.eclipse.birt.report.data.oda.mongodb.1.1.0.zip](http://raw.github.com/maihde/mongo-oda-birt/master/org.eclipse.birt.report.data.oda.mongodb.1.1.0.zip)


License
=======

Eclipse Public License 1.0
Copyright (C) 2010-2012 Pulak Bose
Copyright (C) 2013 Michael Ihde
