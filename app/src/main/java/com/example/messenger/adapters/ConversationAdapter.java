package com.example.messenger.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messenger.databinding.ItemContainerRecentConversationsBinding;
import com.example.messenger.listeners.ConversationListener;
import com.example.messenger.models.ChatMessage;
import com.example.messenger.models.User;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>{

    private final List<ChatMessage> chatMessages;
    private final ConversationListener conversationListener;

    public ConversationAdapter(List<ChatMessage> chatMessages, ConversationListener conversationListener) {
        this.chatMessages = chatMessages;
        this.conversationListener = conversationListener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(ItemContainerRecentConversationsBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {

        private ItemContainerRecentConversationsBinding binding;

        ConversationViewHolder(ItemContainerRecentConversationsBinding itemContainerRecentConversationsBinding) {
            super(itemContainerRecentConversationsBinding.getRoot());
            binding = itemContainerRecentConversationsBinding;
        }

        private void setData(ChatMessage chatMessage){
            binding.imageProfile.setImageBitmap(getConversationImage(chatMessage.conversationImage));
            binding.textName.setText(chatMessage.conversationName);
            binding.textMessage.setText(chatMessage.message);
            User user = new User();
            user.id = chatMessage.conversationId;
            user.name = chatMessage.conversationName;
            user.image = chatMessage.conversationImage;
            binding.getRoot().setOnClickListener(v -> {
                conversationListener.onConversationClicked(user);
            });
        }
    }

    private Bitmap getConversationImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
    }

}
