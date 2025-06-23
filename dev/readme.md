# development tools

## Getting Started
For ease of use, a docker-compose file is provided to set up the development environment.  
You can run the `Start Dependencies` Configuration in IntelliJ to start mongo and minio.  
To start Fill in the development profile, run the `Start Fill` Configuration.

## Populate the Database

Copy the code from the `mongo.js` file into the MongoDB shell to populate the database with example data.

## URLs
* Minio: [http://localhost:9001](http://localhost:9001) (`fill_root:fill_pass`)
* MongoDB: `mongodb://localhost:27017/fill`
* Fill: [http://localhost:8080](http://localhost:8080)
* GraphiQL: [http://localhost:8080/graphiql](http://localhost:8080/graphiql)

## Scripts

* Example HTTP requests: [test.http](test.http)
* Example GraphQL query: [test.graphql](test.graphql)
