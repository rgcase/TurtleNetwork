package com.wavesplatform.http

import akka.http.scaladsl.server.{Directive, Route}
import com.wavesplatform.api.http.ApiError.DiscontinuedApi
import com.wavesplatform.api.http.assets.TransferV1Request
import com.wavesplatform.api.http.{ApiRoute, WithSettings}
import com.wavesplatform.settings.RestAPISettings
import com.wavesplatform.transaction.TransactionFactory
import com.wavesplatform.utils.Time
import com.wavesplatform.utx.UtxPool
import com.wavesplatform.wallet.Wallet
import io.netty.channel.group.ChannelGroup
import io.swagger.annotations._
import javax.ws.rs.Path

@Path("/TN")
@Api(value = "TN")
@Deprecated
case class WavesApiRoute(settings: RestAPISettings, wallet: Wallet, utx: UtxPool, allChannels: ChannelGroup, time: Time)
    extends ApiRoute
    with BroadcastRoute
    with WithSettings {

  override lazy val route = pathPrefix("turtlenode") {
    externalPayment ~ signPayment ~ broadcastSignedPayment ~ payment ~ createdSignedPayment
  }

  @Deprecated
  @Path("/payment")
  @ApiOperation(
    value = "Send payment from wallet. Deprecated: use /assets/transfer instead",
    notes = "Send payment from wallet to another wallet. Each call sends new payment. Deprecated: use /assets/transfer instead",
    httpMethod = "POST",
    produces = "application/json",
    consumes = "application/json"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "body",
        value = "Json with data",
        required = true,
        paramType = "body",
        dataType = "com.wavesplatform.api.http.assets.TransferV1Request",
        defaultValue = "{\n\t\"amount\":400,\n\t\"fee\":1,\n\t\"sender\":\"senderId\",\n\t\"recipient\":\"recipientId\"\n}"
      )
    ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Json with response or error")))
  def payment: Route = (path("payment") & post & withAuth) {
    json[TransferV1Request] { payment =>
      doBroadcast(TransactionFactory.transferAssetV1(payment, wallet, time))
    }
  }

  @Deprecated
  @Path("/payment/signature")
  def signPayment: Route = discontinued(path("payment" / "signature"))

  @Deprecated
  @Path("/create-signed-payment")
  def createdSignedPayment: Route = discontinued(path("create-signed-payment"))

  @Deprecated
  @Path("/external-payment")
  def externalPayment: Route = discontinued(path("external-payment"))

  @Deprecated()
  @Path("/broadcast-signed-payment")
  def broadcastSignedPayment: Route = discontinued(path("broadcast-signed-payment"))

  private[this] def discontinued(path: Directive[Unit]): Route =
    (path & post) (complete(DiscontinuedApi))
}
