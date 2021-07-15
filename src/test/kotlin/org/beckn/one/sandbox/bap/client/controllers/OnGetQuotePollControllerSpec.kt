package org.beckn.one.sandbox.bap.client.controllers

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.beckn.one.sandbox.bap.client.dtos.ClientQuoteResponse
import org.beckn.one.sandbox.bap.client.services.GenericOnPollService
import org.beckn.one.sandbox.bap.errors.database.DatabaseError
import org.beckn.one.sandbox.bap.message.entities.MessageDao
import org.beckn.one.sandbox.bap.message.entities.OnSelectDao
import org.beckn.one.sandbox.bap.message.factories.ProtocolOnSelectMessageSelectedFactory
import org.beckn.one.sandbox.bap.message.mappers.ContextMapper
import org.beckn.one.sandbox.bap.message.mappers.OnSelectResponseMapper
import org.beckn.one.sandbox.bap.message.repositories.BecknResponseRepository
import org.beckn.one.sandbox.bap.message.repositories.GenericRepository
import org.beckn.one.sandbox.bap.schemas.ProtocolOnSelect
import org.beckn.one.sandbox.bap.schemas.ProtocolOnSelectMessage
import org.beckn.one.sandbox.bap.schemas.factories.ContextFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(value = ["test"])
@TestPropertySource(locations = ["/application-test.yml"])
internal class OnGetQuotePollControllerSpec @Autowired constructor(
  private val selectResponseRepo: BecknResponseRepository<OnSelectDao>,
  private val messageRepository: GenericRepository<MessageDao>,
  private val onSelectResponseMapper: OnSelectResponseMapper,
  private val contextMapper: ContextMapper,
  private val contextFactory: ContextFactory,
  private val mapper: ObjectMapper,
  private val mockMvc: MockMvc
) : DescribeSpec() {

  private val fixedClock = Clock.fixed(
    Instant.parse("2018-11-30T18:35:24.00Z"),
    ZoneId.of("UTC")
  )

  val context = contextFactory.create()
  private val contextDao = contextMapper.fromSchema(context)
  private val anotherMessageId = "d20f481f-38c6-4a29-9acd-cbd1adab9ca0"
  private val protocolOnSelect = ProtocolOnSelect(
    context,
    message = ProtocolOnSelectMessage(selected = ProtocolOnSelectMessageSelectedFactory.create())
  )

  init {
    describe("OnGetQuote callback") {
      selectResponseRepo.clear()
      messageRepository.insertOne(MessageDao(id = contextDao.messageId, type = MessageDao.Type.Select))
      selectResponseRepo.insertMany(entitySelectResults())

      context("when called for given message id") {
        val onGetQuoteCall = mockMvc
          .perform(
            MockMvcRequestBuilders.get("/client/v1/on_get_quote")
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .param("messageId", contextDao.messageId)
          )

        it("should respond with status ok") {
          onGetQuoteCall.andExpect(status().isOk)
        }

        it("should respond with all select responses in body") {
          val results = onGetQuoteCall.andReturn()
          val body = results.response.contentAsString
          val clientResponse = mapper.readValue(body, ClientQuoteResponse::class.java)
          clientResponse.message?.quote shouldNotBe null
          clientResponse.message?.quote shouldBe protocolOnSelect.message?.selected?.quote
        }
      }

      context("when failure occurs during request processing") {
        val mockOnPollService = mock<GenericOnPollService<ProtocolOnSelect, ClientQuoteResponse>> {
          onGeneric { onPoll(any()) }.thenReturn(Either.Left(DatabaseError.OnRead))
        }
        val onSelectPollController = OnGetQuotePollController(mockOnPollService, contextFactory)
        it("should respond with failure") {
          val response = onSelectPollController.onSelect(contextDao.messageId)
          response.statusCode shouldBe DatabaseError.OnRead.status()
        }
      }
    }
  }

  fun entitySelectResults(): List<OnSelectDao> {
    val onSelectDao = onSelectResponseMapper.protocolToEntity(protocolOnSelect)
    return listOf(
      onSelectDao,
      onSelectDao,
      onSelectDao.copy(context = contextDao.copy(messageId = anotherMessageId))
    )
  }
}