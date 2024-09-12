package com.ramsvidor

import cats.effect.*
import cats.effect.kernel.Ref
import cats.syntax.all.*
import com.comcast.ip4s.*
import com.ramsvidor.api.BlockchainAPI
import com.ramsvidor.api.BlockchainAPI.routes
import com.ramsvidor.app.Blockchain
import com.ramsvidor.app.Blockchain.Ledger
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.Logger

import scala.collection.immutable.Queue

object Main extends IOApp.Simple {

  override def run: IO[Unit] = for {
    ref <- Ref.of[IO, Ledger[IO]](Ledger(Vector.empty, Queue.empty))
    blockchain = Blockchain[IO](ref)
    _ <- server(blockchain).useForever
  } yield ()

  def server(blockchain: Blockchain[IO]): Resource[IO, Unit] = {
    EmberServerBuilder.default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(Logger.httpApp(true, true)(Router("/api" -> BlockchainAPI.routes(blockchain)).orNotFound))
      .build
      .void
  }

}
