using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using WebAPI.Helpers;

namespace WebAPI.Models
{
    public class SkuciSeDBContext : DbContext
    {
        private const int seed = 0;
        public SkuciSeDBContext(DbContextOptions<SkuciSeDBContext> options) : base(options)
        {
        }

        public DbSet<User> Users { get; set; }
        public DbSet<Advert> Adverts { get; set; }
        public DbSet<Message> Messages { get; set; }
        public DbSet<FavouriteAdvert> FavouriteAdverts { get; set; }
        public DbSet<Meeting> Meetings { get; set; }
        public DbSet<Review> Reviews { get; set; }

        public static List<string> CityNames = new List<string>() { "Beograd", "Novi Sad", "Niš", "Kragujevac",
                                                                "Priština", "Subotica", "Zrenjanin", "Pančevo",
                                                                "Čačak", "Kruševac", "Kraljevo", "Novi Pazar",
                                                                "Smederevo", "Leskovac", "Užice", "Vranje", "Valjevo",
                                                                "Šabac", "Sombor", "Požarevac", "Pirot", "Zaječar",
                                                                "Kikinda", "Sremska Mitrovica", "Jagodina", "Vršac",
                                                                "Bor", "Prokuplje", "Loznica"};

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            modelBuilder.Entity<User>().HasIndex(u => u.Email).IsUnique();
            modelBuilder.Entity<User>().HasIndex(u => u.Username).IsUnique();

            modelBuilder.Entity<FavouriteAdvert>().HasKey(fa => new { fa.AdvertId, fa.UserId });

            SkuciSeDBSeed.Seed(modelBuilder, seed);
        }
    }
}
