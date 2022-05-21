using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace WebAPI.Models
{
    public class FavouriteAdvert
    {
        public uint UserId { get; set; }
        public uint AdvertId { get; set; }
    }
}
