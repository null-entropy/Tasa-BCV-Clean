package com.tasabcv.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

sealed class SingleRateState {
    data object Loading : SingleRateState()

    data class Loaded(
        val rate: DolarRate,
        val isFromCache: Boolean,
        val warning: String? = null
    ) : SingleRateState()

    data class Empty(val errorMessage: String) : SingleRateState()
}

data class DolarUiState(
    val usdState: SingleRateState = SingleRateState.Loading,
    val eurState: SingleRateState = SingleRateState.Loading
)

class DolarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DolarRepository(application)

    private val _uiState = MutableStateFlow(DolarUiState())
    val uiState: StateFlow<DolarUiState> = _uiState.asStateFlow()

    /** True mientras hay una descarga de red en curso; independiente del estado mostrado. */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var hasLoadedOnce = false

    /**
     * Primer abrir (v4.3.1): muestra la caché al instante (sin pantalla vacía) y
     * refresca en background SOLO si la caché tiene más de 30 min o nunca hubo
     * fetch. Con caché reciente no se golpea la red: el BCV publica un valor
     * por día hábil, no tiene sentido re-descargar si abres la app a los 5 min.
     */
    fun loadOnFirstOpen() {
        if (hasLoadedOnce) return
        hasLoadedOnce = true
        showCacheNow()
        val lastFetch = repository.getLastSuccessfulFetchMillis()
        val elapsed = System.currentTimeMillis() - lastFetch
        if (lastFetch == 0L || elapsed >= REVALIDATE_INTERVAL_MS) {
            refresh()
        }
    }

    /**
     * Llamado cuando la app vuelve a primer plano. Revalida la red solo si pasó
     * suficiente tiempo desde el último fetch exitoso (un valor por día hábil:
     * no tiene sentido consultar más seguido). Máximo unas pocas consultas al día.
     */
    fun onAppResumed() {
        if (!hasLoadedOnce) return
        val lastFetch = repository.getLastSuccessfulFetchMillis()
        val elapsed = System.currentTimeMillis() - lastFetch
        if (lastFetch == 0L || elapsed >= REVALIDATE_INTERVAL_MS) {
            refresh()
        }
    }

    /**
     * Sincronización total (v4.3.1): las tasas visibles se MANTIENEN (caché o
     * dato fresco) mientras USD y EUR se descargan en paralelo; los estados se
     * actualizan JUNTOS solo cuando ambas terminaron. El flag `isRefreshing`
     * (independiente del estado mostrado) alimenta el botón de actualizar y
     * bloquea dobles fetch.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val usdDeferred = async(Dispatchers.IO) { repository.fetchRate("USD") }
                val eurDeferred = async(Dispatchers.IO) { repository.fetchRate("EUR") }

                val usdResult = usdDeferred.await()
                val eurResult = eurDeferred.await()

                _uiState.update {
                    it.copy(
                        usdState = mapResultToState(usdResult),
                        eurState = mapResultToState(eurResult)
                    )
                }

                if (usdResult is DolarResult.Success || eurResult is DolarResult.Success) {
                    DolarWidgetProvider.refreshAll(getApplication())
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Pinta al instante la última tasa guardada (lectura SharedPreferences,
     * ~0 ms). Las monedas sin caché quedan en Loading (la descarga está en
     * curso); una caché inválida (sanity check) también se ignora.
     * La banda "Sin conexión" NO se muestra aquí: aún no sabemos si hay fallo;
     * aparece solo si el fetch en background termina en error.
     */
    private fun showCacheNow() {
        val cachedUsd = repository.getCachedRate("USD")
        val cachedEur = repository.getCachedRate("EUR")
        _uiState.value = DolarUiState(
            usdState = cachedUsd?.let { SingleRateState.Loaded(it, isFromCache = true) }
                ?: SingleRateState.Loading,
            eurState = cachedEur?.let { SingleRateState.Loaded(it, isFromCache = true) }
                ?: SingleRateState.Loading
        )
    }

    private companion object {
        /** Umbral de revalidación al volver a primer plano: 30 minutos. */
        const val REVALIDATE_INTERVAL_MS = 30L * 60L * 1000L
    }

    private fun mapResultToState(result: DolarResult): SingleRateState {
        return when (result) {
            is DolarResult.Success -> {
                SingleRateState.Loaded(
                    rate = result.rate,
                    isFromCache = false
                )
            }
            is DolarResult.Error -> {
                val cached = result.cachedRate
                if (cached != null) {
                    SingleRateState.Loaded(
                        rate = cached,
                        isFromCache = true,
                        warning = "Sin conexión: mostrando el último dato guardado."
                    )
                } else {
                    SingleRateState.Empty(result.errorMessage)
                }
            }
        }
    }
}
