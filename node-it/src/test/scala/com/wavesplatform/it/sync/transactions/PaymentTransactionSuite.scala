package com.wavesplatform.it.sync.transactions

import com.wavesplatform.it.api.SyncHttpApi._
import com.wavesplatform.it.api.PaymentRequest
import com.wavesplatform.it.transactions.BaseTransactionSuite
import com.wavesplatform.it.util._
import org.scalatest.prop.TableDrivenPropertyChecks

class PaymentTransactionSuite extends BaseTransactionSuite with TableDrivenPropertyChecks {

  private val paymentAmount = 5.TN
  private val defaulFee     = 1.TN

  test("TN payment changes TN balances and eff.b.") {

    val (firstBalance, firstEffBalance)   = miner.accountBalances(firstAddress)
    val (secondBalance, secondEffBalance) = miner.accountBalances(secondAddress)

    val transferId = sender.payment(firstAddress, secondAddress, paymentAmount, defaulFee).id
    nodes.waitForHeightAriseAndTxPresent(transferId)
    miner.assertBalances(firstAddress, firstBalance - paymentAmount - defaulFee, firstEffBalance - paymentAmount - defaulFee)
    miner.assertBalances(secondAddress, secondBalance + paymentAmount, secondEffBalance + paymentAmount)
  }

  val payment = PaymentRequest(5.TN, 1.TN, firstAddress, secondAddress)
  val endpoints =
    Table("/TN/payment/signature", "/TN/create-signed-payment", "/TN/external-payment", "/TN/broadcast-signed-payment")
  forAll(endpoints) { (endpoint: String) =>
    test(s"obsolete endpoints respond with BadRequest. Endpoint:$endpoint") {
      val errorMessage = "This API is no longer supported"
      assertBadRequestAndMessage(sender.postJson(endpoint, payment), errorMessage)
    }
  }
}
