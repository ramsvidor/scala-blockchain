package com.ramsvidor.api

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Ref}
import com.ramsvidor.api.APISpec.*
import com.ramsvidor.app.Blockchain
import com.ramsvidor.app.Blockchain.Ledger
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.scalatest.funsuite.AsyncFunSuite

import scala.collection.immutable.Queue

class BlockchainAPISpec extends AsyncFunSuite with AsyncIOSpec {

  private def blockchain = Blockchain[IO](Ref.unsafe(Ledger[IO](Vector.empty, Queue.empty)))

  private val privateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgtjpmdQ4YxNDkx5KvqaPxCxgsIyleLlAV2H929nh" +
    "8lnugCgYIKoZIzj0DAQehRANCAARFOTn+j1FZ8HibFiHJiknTgPrh400EBakC+EGBankk72PmZZhdVRajil4OaTVUGXS3gZYjjvfv+K2e26y7D37n"
  private val publicKey1 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAERTk5/o9RWfB4mxYhyYpJ04D64eNNBAWpAvhBgWp5JO9j5mWYXVU" +
    "Wo4peDmk1VBl0t4GWI4737/itntusuw9+5w=="
  private val publicKey2 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEzPpO8fENAL05kzYJK+3+VxBEJA+9IJYma8adZX8GR0pTojt6i2G" +
    "FZ1bsGAzoev3B9khYuIm4dcc+bYMr6MZZgw=="
  private val faucetRequest = FaucetRequest(publicKey1, 10).asJson

  def postRequest(uri: Uri, json: io.circe.Json): Request[IO] =
    Request[IO](Method.POST, uri).withEntity(json)

  test("POST /wallet creates a new wallet") {
    val req = Request[IO](Method.POST, uri"/wallet")
    val routes = BlockchainAPI.routes(blockchain)

    routes.run(req).value.map {
      case Some(resp) => assert(resp.status == Status.Ok)
      case None => fail("Request was not handled")
    }
  }

  test("POST /faucet requests funds for a wallet") {
    val req = postRequest(uri"/faucet", faucetRequest)
    val routes = BlockchainAPI.routes(blockchain)

    routes.run(req).value.map {
      case Some(resp) => assert(resp.status == Status.Ok)
      case None => fail("Request was not handled")
    }
  }

  test("POST /transfer processes a transfer between wallets") {
    // It is necessary to request funds for the wallets before transferring
    val faucetReq = postRequest(uri"/faucet", faucetRequest)
    val transferReq = postRequest(uri"/transfer", TransferRequest(privateKey, publicKey1, publicKey2, 5).asJson)
    val routes = BlockchainAPI.routes(blockchain)

    for {
      faucetResp <- routes.run(faucetReq).value
      _ = assert(faucetResp.exists(_.status == Status.Ok), "Faucet request failed")
      transferResp <- routes.run(transferReq).value
      _ = assert(transferResp.exists(_.status == Status.Ok), "Transfer request failed")
    } yield ()
  }

  test("GET /wallet/{publicKey}/balance returns the wallet balance") {
    val req = Request[IO](Method.GET, uri"/wallet/publicKeySample/balance")
    val routes = BlockchainAPI.routes(blockchain)

    routes.run(req).value.map {
      case Some(resp) => assert(resp.status == Status.Ok)
      case None => fail("Request was not handled")
    }
  }

  test("GET /block returns the current blockchain state") {
    val req = Request[IO](Method.GET, uri"/block")
    val routes = BlockchainAPI.routes(blockchain)

    routes.run(req).value.map {
      case Some(resp) => assert(resp.status == Status.Ok)
      case None => fail("Request was not handled")
    }
  }
}
