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

  private val privateKey = "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgwN6BfFPwF41FNIJIR5OKJSNM1N7Cp+15KylNl+hp2sy" +
    "gBwYFK4EEAAqhRANCAARIgNygVPdBznn+MEcb/vryalAraX1EEFBv1/lPDgER/qd6oW8YEOSycCwMqxobZA6XxKyTkc2LP8cwRHZQZwOs"
  private val publicKey1 = "024880dca054f741ce79fe30471bfefaf26a502b697d4410506fd7f94f0e0111fe"
  private val publicKey2 = "03d9ad7b79ac62de42200b7e238332d3c943a1aec1e3afe5954a27777171e99c43"
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
