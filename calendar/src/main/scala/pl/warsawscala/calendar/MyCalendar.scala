package pl.warsawscala.calendar

import java.time.LocalDate

import scala.concurrent.Future
import play.api.libs.ws.WSClient

// import play.api.libs.ws.DefaultWSClientConfig
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient, NingWSClientConfig}

import scala.concurrent.Future

case class PlannedEvent(startDate: LocalDate, endDateExclusive: LocalDate, tags: Seq[String])

case class GoogleEvent(startDate: LocalDate, endDate: LocalDate, summary: String)

trait MyCalendar {
  def getEventsFor(from: LocalDate, to: LocalDate): Future[Seq[PlannedEvent]] // ???
}

case class MyCalendarImpl(code: String, client: WSClient) extends MyCalendar {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getEventsFor(from: LocalDate, to: LocalDate): Future[Seq[PlannedEvent]] = {
    getAuthToken flatMap {
      getCalendarEntries(_) flatMap { s => s }
    }
  }

  def getAuthToken: Future[String] = {
    val postData: Map[String, Seq[String]] = Map("code" -> code,
      "client_id" -> "468805955202-t2ahu8an02kmvc13pk15adp8to1nitc0.apps.googleusercontent.com",
      "client_secret" -> "YQig_XAAns1PlEdS7XNXqwB6",
      "redirect_uri" -> "http://localhost:9000/oauth2callback",
      "grant_type" -> "authorization_code") map { case (key, value) => (key, Seq(value)) }

    client.url("https://www.googleapis.com/oauth2/v4/token")
      .withHeaders(("Content-Type", "application/x-www-form-urlencoded"))
      .post(postData) map {
      response => (response.json \ "access_token").as[String]
    }
  }

  def getCalendarEntries(authToken: String) = {
    client.url("https://www.googleapis.com/calendar/v3/users/me/calendarList")
      .withQueryString("access_token" -> authToken)
      .get() map {
      response =>
        println("Calendar API response list: " + response.json.toString())
        MyCalendarStub().getEventsFor(LocalDate.now(), LocalDate.now())
    }
  }

  def ParseEvents(EventsList: List[GoogleEvent]): List[PlannedEvent] = {
    EventsList.map(e => PlannedEvent(e.startDate, e.endDate, ParseTags(e.summary)))
  }

  def ParseTags(summary: String): List[String] = {
    if (summary.startsWith("#")) {
      summary.split("#").zipWithIndex.filter(_._2 % 2 == 0).map(_._1).toList
    } else {
      summary.split("#").zipWithIndex.filter(_._2 % 2 == 1).map(_._1).toList
    }
  }
}

case class MyCalendarStub() extends MyCalendar {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getEventsFor(from: LocalDate, to: LocalDate): Future[Seq[PlannedEvent]] = {
    Future {
      val myPlannedEvent: PlannedEvent = PlannedEvent(LocalDate.now(), LocalDate.now(), List("holiday", "icecreamdays"))
      val myPlannedEvent2: PlannedEvent = PlannedEvent(LocalDate.now(), LocalDate.now(), List("grilldays"))
      val myPlannedEvent3: PlannedEvent = PlannedEvent(LocalDate.now(), LocalDate.now(), List("holiday"))
      List(myPlannedEvent, myPlannedEvent2, myPlannedEvent3)
    }
  }
}

object MyCalendar {
  def apply(code: String, client: WSClient): MyCalendar = MyCalendarImpl(code, client)
}

