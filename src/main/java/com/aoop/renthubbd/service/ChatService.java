package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.MessagePayload;
import com.aoop.renthubbd.model.*;
import com.aoop.renthubbd.repository.ConversationRepository;
import com.aoop.renthubbd.repository.MessageRepository;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       PropertyRepository propertyRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Conversation getOrCreate(Long propertyId, Long renterId) {
        Conversation existing = conversationRepository
                .findByPropertyIdAndRenterId(propertyId, renterId)
                .orElse(null);
        if (existing != null) return existing;

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));

        if (property.getOwner() == null)
            throw new IllegalArgumentException("Listing has no owner.");

        if (property.getOwner().getId().equals(renterId))
            throw new IllegalArgumentException("You cannot message yourself.");

        User renter = userRepository.findById(renterId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Conversation c = new Conversation();
        c.setProperty(property);
        c.setRenter(renter);
        c.setOwner(property.getOwner());
        return conversationRepository.save(c);
    }

    @Transactional
    public Message send(Long conversationId, Long senderId, String content) {
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Message cannot be empty.");
        if (content.length() > 2000)
            throw new IllegalArgumentException("Message is too long (max 2000 characters).");

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

        boolean isRenter = conv.getRenter().getId().equals(senderId);
        boolean isOwner  = conv.getOwner().getId().equals(senderId);
        if (!isRenter && !isOwner)
            throw new IllegalArgumentException("You are not part of this conversation.");

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found."));

        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setContent(content.trim());
        Message saved = messageRepository.save(msg);

        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        dispatchMessageNotifications(conv, sender, content);
        return saved;
    }

    @Async
    public void dispatchMessageNotifications(Conversation conv, User sender, String content) {
        Long recipientId = conv.getRenter().getId().equals(sender.getId())
                ? conv.getOwner().getId()
                : conv.getRenter().getId();

        notificationService.notifyUser(
                recipientId,
                NotificationType.NEW_MESSAGE,
                "New message from " + sender.getFirstName(),
                "About \"" + conv.getProperty().getTitle() + "\": "
                        + (content.length() > 120 ? content.substring(0, 117) + "..." : content),
                conv.getProperty().getId());
    }

    public List<Conversation> listForRenter(Long renterId) {
        return conversationRepository.findByRenterIdOrderByLastMessageAtDesc(renterId);
    }

    public List<Conversation> listForOwner(Long ownerId) {
        return conversationRepository.findByOwnerIdOrderByLastMessageAtDesc(ownerId);
    }

    public Conversation getForUser(Long conversationId, Long userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
        if (!conv.getRenter().getId().equals(userId) && !conv.getOwner().getId().equals(userId))
            throw new IllegalArgumentException("Access denied.");
        return conv;
    }

    public List<Message> messages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    public List<Message> messagesAfter(Long conversationId, Long afterId) {
        if (afterId == null || afterId <= 0L) return messages(conversationId);
        return messageRepository
                .findByConversationIdAndIdGreaterThanOrderBySentAtAsc(conversationId, afterId);
    }

    public List<MessagePayload> toPayloads(List<Message> messages, Long viewerId) {
        List<MessagePayload> out = new ArrayList<>();
        for (Message m : messages) {
            MessagePayload p = new MessagePayload();
            p.setId(m.getId());
            p.setSenderId(m.getSender().getId());
            p.setSenderName(m.getSender().getFirstName());
            p.setContent(m.getContent());
            p.setSentAt(m.getSentAt());
            p.setMine(m.getSender().getId().equals(viewerId));
            out.add(p);
        }
        return out;
    }

    @Transactional
    public void markRead(Long conversationId, Long readerId) {
        List<Message> messages = messageRepository
                .findByConversationIdOrderBySentAtAsc(conversationId);
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (Message m : messages) {
            if (m.getReadAt() == null && !m.getSender().getId().equals(readerId)) {
                m.setReadAt(now);
                messageRepository.save(m);
                changed = true;
            }
        }
    }

    public long unreadCount(Long conversationId, Long readerId) {
        return messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(conversationId, readerId);
    }


    public long totalUnreadForUser(Long userId) {
        return messageRepository.countAllUnreadForUser(userId);
    }

    public long unreadInConversation(Long conversationId, Long readerId) {
        return messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(conversationId, readerId);
    }
}