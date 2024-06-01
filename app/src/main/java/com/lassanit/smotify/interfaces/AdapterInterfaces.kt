package com.lassanit.smotify.interfaces

interface AdapterInterfaces {
    interface Recycler{
        fun reload()
        fun itemAdded(pkg: String, title: String)
    }
}