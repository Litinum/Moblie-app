using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;

namespace WebAPI.Models
{
    public class Meeting
    {
        [Key]
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public uint Id { get; set; }
        public uint AdvertId { get; set; }
        public uint VisitorId { get; set; }
        public DateTime Time { get; set; }
        public DateTime DateCreated { get; set; }
        public bool AgreedVisitor { get; set; }
        public bool AgreedOwner { get; set; }
        public bool Concluded { get; set; }
    }
}
