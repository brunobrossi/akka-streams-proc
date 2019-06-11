package com.simplaex.model

import java.util.UUID

case class Message(userAccount: UUID, base64Encoded: String, floatPoint: Double, intValue1: Int, intValue2: BigInt ) {
    //val base64Decoded = new String(java.util.Base64.getDecoder.decode(base64Encoded))
}
