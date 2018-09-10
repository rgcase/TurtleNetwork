package com.wavesplatform.matcher.api

import com.wavesplatform.database.{DBExt, RW}
import com.wavesplatform.matcher.MatcherKeys
import com.wavesplatform.matcher.model.OrderInfo
import com.wavesplatform.state.ByteStr
import org.iq80.leveldb.DB
import scorex.account.Address
import scorex.transaction.AssetId
import scorex.transaction.assets.exchange.{AssetPair, Order}

object DBUtils {
  import OrderInfo.orderStatusOrdering

  def ordersByAddressAndPair(db: DB, address: Address, pair: AssetPair, maxOrders: Int): Seq[(Order, OrderInfo)] = db.readOnly { ro =>
    (for {
      idx     <- (db.get(MatcherKeys.addressOrdersByPairSeqNr(address, pair)) to 0 by -1).view
      orderId <- ro.get(MatcherKeys.addressOrdersByPair(address, pair, idx))
      order   <- ro.get(MatcherKeys.order(orderId))
      orderInfo = ro.get(MatcherKeys.orderInfo(orderId))
    } yield (order, orderInfo)).take(maxOrders).sortBy { case (order, info) => (info.status, -order.timestamp) }
  }

  def ordersByAddress(db: DB, address: Address, changedAssets: Set[Option[AssetId]], activeOnly: Boolean, maxOrders: Int): Seq[(Order, OrderInfo)] =
    db.readOnly { ro =>
      val fromIndex = db.get(MatcherKeys.addressOrdersSeqNr(address))
      val toIndex = math.min(
        math.max(1, fromIndex - maxOrders + 1),
        db.get(MatcherKeys.addressOldestActiveOrderSeqNr(address)).getOrElse(Int.MaxValue)
      )

      (for {
        idx             <- (fromIndex to toIndex by -1).view
        orderSpendAsset <- ro.get(MatcherKeys.addressOrders(address, idx))
        if changedAssets.isEmpty || changedAssets(orderSpendAsset.spendAsset)
        order <- ro.get(MatcherKeys.order(orderSpendAsset.orderId))
        orderInfo = ro.get(MatcherKeys.orderInfo(orderSpendAsset.orderId))
        if !(activeOnly && orderInfo.status.isFinal)
      } yield (order, orderInfo)).sortBy { case (order, info) => (info.status, -order.timestamp) }.take(maxOrders)
    }

  def reservedBalance(db: DB, address: Address): Map[Option[AssetId], Long] = db.readOnly { ro =>
    (for {
      idx <- 1 to ro.get(MatcherKeys.openVolumeSeqNr(address))
      assetId = ro.get(MatcherKeys.openVolumeAsset(address, idx))
      volume <- ro.get(MatcherKeys.openVolume(address, assetId))
      if volume != 0
    } yield assetId -> volume).toMap
  }

  def order(db: DB, orderId: ByteStr): Option[Order] = db.get(MatcherKeys.order(orderId))

  def orderInfo(db: DB, orderId: ByteStr): OrderInfo = db.get(MatcherKeys.orderInfo(orderId))
  def orderInfo(rw: RW, orderId: ByteStr): OrderInfo = rw.get(MatcherKeys.orderInfo(orderId))
}
