package com.lojasocial.app.data.repository

import com.lojasocial.app.R
import com.lojasocial.app.domain.PendingRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PendingRequestsRepository @Inject constructor() {
    fun getPendingRequests(): Flow<List<PendingRequest>> = flow {
        delay(1500)

        val fakeRequests = listOf(
            PendingRequest(1, "José Alves (DB)", "2h atrás", "Vários", R.drawable.basket_fill),
            PendingRequest(2, "Enrique Rodrigues (DB)", "3h atrás", "Limpeza", R.drawable.mop),
            PendingRequest(3, "Diogo Machado (DB)", "8h atrás", "Alimentar", R.drawable.cutlery),
            PendingRequest(4, "Carlos Barreiro (DB)", "14h atrás", "Higiene", R.drawable.perfume),
            PendingRequest(5, "Maria Silva (DB)", "1d atrás", "Vários", R.drawable.basket_fill)
        )
        emit(fakeRequests)
    }
}