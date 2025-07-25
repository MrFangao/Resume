package com.example.drawingapp.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.drawingapp.databinding.FragmentColorPickerBinding

class ColorPickerFragment : DialogFragment() {

    interface ColorPickerListener {
        fun onColorSelected(color: Int)
    }

    private var listener: ColorPickerListener? = null
    private var _binding: FragmentColorPickerBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            context as ColorPickerListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context 必须实现 ColorPickerListener 接口")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 设置颜色按钮的点击事件
        binding.RedButton.setOnClickListener {
            listener?.onColorSelected(2)
            dismiss()
        }
        binding.MagentaButton.setOnClickListener {
            listener?.onColorSelected(3)
            dismiss()
        }
        binding.YellowButton.setOnClickListener {
            listener?.onColorSelected(4)
            dismiss()
        }
        binding.GreenButton.setOnClickListener {
            listener?.onColorSelected(5)
            dismiss()
        }

        binding.BlueButton.setOnClickListener {
            listener?.onColorSelected(6)
            dismiss()
        }
        binding.CyanButton.setOnClickListener {
            listener?.onColorSelected(7)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
