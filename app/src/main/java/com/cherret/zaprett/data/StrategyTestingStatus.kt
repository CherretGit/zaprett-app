package com.cherret.zaprett.data

import com.cherret.zaprett.R

enum class StrategyTestingStatus(val resId: Int) {
    Waiting(R.string.strategy_status_waiting), Testing(R.string.strategy_status_testing), Completed(R.string.strategy_status_tested)
}