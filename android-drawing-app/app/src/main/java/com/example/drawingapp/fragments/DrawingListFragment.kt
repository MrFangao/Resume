// DrawingListFragment.kt
package com.example.drawingapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.activityViewModels
import com.example.drawingapp.room.CanvasEntity
import com.example.drawingapp.DrawingViewModel
import com.example.drawingapp.DrawingViewModelFactory
import com.example.drawingapp.room.CanvasApplication

class DrawingListFragment : Fragment() {
    private val viewModel: DrawingViewModel by activityViewModels {
        DrawingViewModelFactory((requireActivity().application as CanvasApplication).canvasRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    DrawingListScreen(viewModel = viewModel)
                }
            }
        }
    }

    @Composable
    fun DrawingListScreen(viewModel: DrawingViewModel = viewModel()) {
        val allCanvases by viewModel.allCanvases.observeAsState(emptyList())
        val currentCanvas by viewModel.currentCanvas.observeAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(allCanvases) { canvas ->
                CanvasItem(
                    canvas = canvas,
                    isSelected = currentCanvas!!.id)
            }
        }
    }

    @Composable
    fun CanvasItem(canvas: CanvasEntity, isSelected : Int) {
        var cardBackgroundColor = MaterialTheme.colorScheme.surface
        if(isSelected == canvas.id) {
            cardBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        }


        Card(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clickable {
                    viewModel.setCurrentCanvas(canvas)
                    Log.d("CanvasItem", "Clicked on canvas with ID: ${canvas.id}")},
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
        ) {
            Row {
                val bitmap = canvas.bitmap.asImageBitmap()
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .height(100.dp)
                        .padding(8.dp))
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(8.dp)
                ) {
                    Text(text = canvas.id.toString(), style = typography.h6)
                    Text(text = canvas.filePath, style = typography.h6)
                }
            }
        }
    }
}






