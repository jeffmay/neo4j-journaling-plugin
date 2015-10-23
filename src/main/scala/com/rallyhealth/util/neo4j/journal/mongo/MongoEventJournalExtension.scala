package com.rallyhealth.util.neo4j.journal.mongo

import javax.ws.rs.core.Context

import com.rallyhealth.util.neo4j.journal.ConsistentEventJournalExtension
import org.neo4j.graphdb.{Path, GraphDatabaseService}
import org.neo4j.server.database.CypherExecutor
import org.neo4j.server.rest.repr.{OutputFormat, InputFormat}

@Path("/journal/mongo")
class MongoEventJournalExtension(
  @Context cypherExecutor: CypherExecutor,
  @Context database: GraphDatabaseService,
  @Context input: InputFormat,
  @Context output: OutputFormat)
  extends ConsistentEventJournalExtension(
    cypherExecutor,
    database,
    input,
    output)
  with MongoJournalBackend  // TODO: Move to @ContextProvider
  with PropertiesBasedJournalConfig
