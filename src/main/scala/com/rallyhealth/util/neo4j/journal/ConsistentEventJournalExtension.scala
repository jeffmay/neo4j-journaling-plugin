package com.rallyhealth.util.neo4j.journal

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{Context, Response}
import javax.ws.rs.{POST, QueryParam}

import org.neo4j.cypher.CypherException
import org.neo4j.graphdb.{GraphDatabaseService, Path}
import org.neo4j.server.database.CypherExecutor
import org.neo4j.server.rest.repr.{CypherResultRepresentation, InputFormat, InvalidArgumentsException, OutputFormat}
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

// TODO: Does this work? Can we use other mechanisms for DI?
abstract class ConsistentEventJournalExtension(
  cypherExecutor: CypherExecutor,
  database: GraphDatabaseService,
  input: InputFormat,
  output: OutputFormat) {
  self: JournalBackend =>

  import ConsistentEventJournalExtension._

  /**
   * Works just like the Cypher endpoint but commits the event to Mongo if successful.
   *
   * Code inspired by [[org.neo4j.server.rest.web.CypherService]]:
   *
     {{{
     public Response cypher(String body,
                            @Context HttpServletRequest request,
                            @QueryParam( INCLUDE_STATS_PARAM ) boolean includeStats,
                            @QueryParam( INCLUDE_PLAN_PARAM ) boolean includePlan,
                            @QueryParam( PROFILE_PARAM ) boolean profile) throws BadInputException {

         Map<String,Object> command = input.readMap( body );

         if( !command.containsKey(QUERY_KEY) ) {
             return output.badRequest( new InvalidArgumentsException( "You have to provide the 'query' parameter." ) );
         }

         String query = (String) command.get( QUERY_KEY );
         Map<String, Object> params;
         try
         {
             params = (Map<String, Object>) (command.containsKey( PARAMS_KEY ) && command.get( PARAMS_KEY ) != null ?
                     command.get( PARAMS_KEY ) :
                     new HashMap<String, Object>());
         }
         catch ( ClassCastException e )
         {
             return output.badRequest( new IllegalArgumentException("Parameters must be a JSON map") );
         }
         try
         {
             QueryExecutionEngine executionEngine = cypherExecutor.getExecutionEngine();
             boolean periodicCommitQuery = executionEngine.isPeriodicCommit( query );
             CommitOnSuccessfulStatusCodeRepresentationWriteHandler handler = (CommitOnSuccessfulStatusCodeRepresentationWriteHandler) this.output.getRepresentationWriteHandler();
             if ( periodicCommitQuery )
             {
                 handler.closeTransaction();
             }

             Result result;
             if ( profile )
             {
                 result = executionEngine.profileQuery( query, params, new ServerQuerySession( request ) );
                 includePlan = true;
             }
             else
             {
                 result = executionEngine.executeQuery( query, params, new ServerQuerySession( request ) );
                 includePlan = result.getQueryExecutionType().requestedExecutionPlanDescription();
             }

             if ( periodicCommitQuery )
             {
                 handler.setTransaction( database.beginTx() );
             }

             return output.ok( new CypherResultRepresentation( result, includeStats, includePlan ) );
         }
         catch ( Throwable e )
         {
             if (e.getCause() instanceof CypherException)
             {
                 return output.badRequest( e.getCause() );
             } else
             {
                 return output.badRequest( e );
             }
         }
     }

     }}}
  */
  @POST
  @Path("/cypher/{cypher}")
  def executeEventWithLogging(
    body: String,
    @Context request: HttpServletRequest,
    @QueryParam(INCLUDE_STATS_PARAM) includeStatsParam: Boolean,
    @QueryParam(INCLUDE_PLAN_PARAM) includePlanParam: Boolean,
    @QueryParam(PROFILE_PARAM) profileParam: Boolean): Response = {
    val json = Json.parse(body)
    val query = (json \ QUERY_KEY).toOption match {
      case Some(JsString(strQuery)) => strQuery
      case Some(other) =>
        val typeName = other.getClass.getSimpleName
        return output.badRequest(new InvalidArgumentsException(s"The 'query' parameter must be a JsString not $typeName."))
      case _ =>
        return output.badRequest(new InvalidArgumentsException( "You have to provide the 'query' parameter."))
    }

    val params: Map[String, AnyRef] = (json \ PARAMS_KEY).toOption match {
      case Some(JsObject(fields)) =>
        fields.mapValues[AnyRef] {
          case JsString(str) => str
          case JsNumber(num) if num.isValidInt => java.lang.Integer.valueOf(num.toIntExact)
          case JsNumber(num) if num.isValidLong => java.lang.Long.valueOf(num.toLongExact)
          case JsNumber(num) => java.lang.Double.valueOf(num.toDouble)  // just force it to a Double or throw an exception
        }.toMap
      case Some(other) =>
        return output.badRequest(new IllegalArgumentException("Parameters must be a JSON map."))
      case None => Map.empty
    }

    Try(database.execute(query, params.asJava)) match {
      case Success(result) =>
        val includePlan = profileParam || result.getQueryExecutionType.requestedExecutionPlanDescription
        //TODO journal
        journal.journal()
        val stats = result.getQueryStatistics
        output.ok(new CypherResultRepresentation(result, includeStatsParam, includePlan))
      case Failure(ex: CypherException) => output.badRequest(ex.getCause)
      case Failure(ex) => output.badRequest(ex)
    }
  }
}
object ConsistentEventJournalExtension {

  private final val PARAMS_KEY: String = "params"
  private final val QUERY_KEY: String = "query"

  private final val INCLUDE_STATS_PARAM: String = "includeStats"
  private final val INCLUDE_PLAN_PARAM: String = "includePlan"
  private final val PROFILE_PARAM: String = "profile"
}
