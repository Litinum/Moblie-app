using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace WebAPI.Models
{
    public class Message
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public uint Id { get; set; }
        public uint SenderId { get; set; }
        public uint ReceiverId { get; set; }
        [MaxLength(280)]
        public string Content { get; set; }
        public DateTime SendDate { get; set; }
        public bool Seen { get; set; }
    }

    public class MessageOrMeeting
    {
        public Message Message { get; set; }
        public MeetingDisplay Meeting { get; set; }
        public bool IsMessage { get; set; }
    }

    public class MeetingDisplay
    {
        public uint id { get; set; }
        public uint advertId { get; set; }
        public uint otherUser { get; set; }
        public string username { get; set; }
        public string title { get; set; }
        public DateTime proposedTime { get; set; }
        public DateTime dateCreated { get; set; }
        public bool agreedVisitor { get; set; }
        public bool agreedOwner { get; set; }
        public bool concluded { get; set; }
        public bool owner { get; set; }
    }
}
