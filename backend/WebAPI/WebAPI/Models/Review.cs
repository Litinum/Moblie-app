using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Threading.Tasks;

namespace WebAPI.Models
{
    public class Review
    {
        [Key]
        public uint MeetingId { get; set; }
        [Range(0, 5)]
        public uint Rating { get; set; }
        public string Text { get; set; }
    }
}
