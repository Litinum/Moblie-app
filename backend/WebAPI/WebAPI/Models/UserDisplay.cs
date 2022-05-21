using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace WebAPI.Models
{
    public struct UserDisplay
    {
        public uint Id { get; set; }
        public string DisplayName { get; set; }
        public bool Online { get; set; }
    }
}
