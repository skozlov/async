package com.github.skozlov.async.cancel

trait Cancellable {
    def cancelled: Boolean
}

object Cancellable {
    trait Mutable extends Cancellable {
        def cancel(): Unit
    }

    trait Volatile extends Mutable {
        @volatile
        private var _cancelled = false

        override def cancelled: Boolean = _cancelled

        override def cancel(): Unit = _cancelled = true
    }
}
