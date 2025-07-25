package com.example.drawingapp.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.drawingapp.databinding.FragmentShapePickerBinding

class ShapePickerFragment : DialogFragment() {

    interface ShapePickerListener {
        fun onShapeSelected(shape: String)
    }

    private var listener: ShapePickerListener? = null
    private var _binding: FragmentShapePickerBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            context as ShapePickerListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context 必须实现 ShapePickerListener 接口")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShapePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置圆形按钮点击事件
        binding.circle.setOnClickListener {
            listener?.onShapeSelected("circle")
            dismiss()  // 关闭 Fragment
        }

        // 设置矩形按钮点击事件
        binding.rectangle.setOnClickListener {
            listener?.onShapeSelected("rectangle")
            dismiss()  // 关闭 Fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
