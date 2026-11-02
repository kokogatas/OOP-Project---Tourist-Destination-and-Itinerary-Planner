package com.example.tdip

// Interface para sa payment processing.
interface PaymentProcessing {
    fun process(amount: Double): Boolean
}

// Payment class para sa transaction details.
open class Payment(
    val paymentId: Int,
    val amount: Double,
    val method: String,
    var status: String = "Pending"
) {
    open fun pay(): Boolean {
        status = "Completed"
        return true
    }
}

// Para sa mga nagbabayad gamit ang credit o debit card.
class CardPayment(
    paymentId: Int,
    amount: Double
) : Payment(paymentId, amount, "Credit/Debit Card")

// Para sa mga nagbabayad ng cash mismo sa resort.
class CashPayment(
    paymentId: Int,
    amount: Double
) : Payment(paymentId, amount, "Cash at Resort")

// Card gateway na nagpo-process ng card payments.
class CardGateway : PaymentProcessing {
    override fun process(amount: Double): Boolean {
        return amount > 0
    }
}
